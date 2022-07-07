/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.RegularMapping
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.ValueParameterTemplate
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassSpecificDeclarations.ImplementationAgnostic
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassSpecificDeclarations.VirtualProperty
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassTree.InternalNode
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassTree.Leaf
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isMultiFieldValueClassType
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.popLast

val jvmMultiFieldValueClassPhase = makeIrFilePhase(
    ::JvmMultiFieldValueClassLowering,
    name = "Multi-field Value Classes",
    description = "Lower multi-field value classes",
    // Collection stubs may require mangling by multi-field value class rules.
    // SAM wrappers may require mangling for fun interfaces with multi-field value class parameters
    prerequisite = setOf(collectionStubMethodLowering, singleAbstractMethodPhase),
)

private class JvmMultiFieldValueClassLowering(context: JvmBackendContext) : JvmValueClassAbstractLowering(context) {

    private open inner class ValueDeclarationsRemapper {
        private val symbol2getter = mutableMapOf<IrValueSymbol, ExpressionGenerator>()
        private val symbol2setters = mutableMapOf<IrValueSymbol, List<ExpressionSupplier?>>()
        private val knownExpressions = mutableMapOf<IrExpression, ImplementationAgnostic>()

        fun remapSymbol(original: IrValueSymbol, replacement: IrValueDeclaration) {
            symbol2getter[original] = { irGet(replacement) }
            symbol2setters[original] = if (replacement.isAssignable) listOf { value -> irSet(replacement, value) } else listOf(null)
        }

        fun remapSymbol(
            original: IrValueSymbol,
            unboxed: List<VirtualProperty>,
            declarations: MultiFieldValueClassSpecificDeclarations
        ): Unit =
            remapSymbol(original, declarations.ImplementationAgnostic(original.owner.type as IrSimpleType, unboxed))

        fun remapSymbol(original: IrValueSymbol, unboxed: ImplementationAgnostic) {
            symbol2getter[original] = {
                unboxed.boxedExpression(this).also { irExpression -> knownExpressions[irExpression] = unboxed }
            }
            symbol2setters[original] = unboxed.virtualFields.map { it.assigner }
        }

        fun IrBuilderWithScope.getter(original: IrValueSymbol): IrExpression? =
            symbol2getter[original]?.invoke(this)

        fun setter(original: IrValueSymbol): List<ExpressionSupplier?>? = symbol2setters[original]

        fun implementationAgnostic(expression: IrExpression): ImplementationAgnostic? = knownExpressions[expression]

        fun IrBuilderWithScope.subfield(expression: IrExpression, name: Name): IrExpression? =
            implementationAgnostic(expression)?.get(name)?.let { (expressionGenerator, representation) ->
                val res = expressionGenerator(this)
                representation?.let { knownExpressions[res] = it }
                res
            }

        /**
         * Register value declaration instead of singular expressions when possible
         */
        fun registerExpression(getter: IrExpression, representation: ImplementationAgnostic) {
            knownExpressions[getter] = representation
        }
    }

    private val valueDeclarationsRemapper = ValueDeclarationsRemapper()

    private val regularClassMFVCPropertyMainGetters: MutableMap<IrClass, MutableMap<Name, IrSimpleFunction>> = mutableMapOf()
    private val regularClassMFVCPropertyNextGetters: MutableMap<IrSimpleFunction, Map<Name, IrSimpleFunction>> = mutableMapOf()
    private val regularClassMFVCPropertyFieldsMapping: MutableMap<IrField, List<IrField>> = mutableMapOf()
    private val regularClassMFVCPropertyNodes: MutableMap<IrSimpleFunction, MultiFieldValueClassTree> = mutableMapOf()
    private val regularClassMFVCPropertyAllPrimitiveGetters: MutableSet<IrSimpleFunction> = mutableSetOf()

    override val replacements
        get() = context.multiFieldValueClassReplacements

    override fun IrClass.isSpecificLoweringLogicApplicable(): Boolean = isMultiFieldValueClass

    override fun IrFunction.isFieldGetterToRemove(): Boolean = isMultiFieldValueClassOriginalFieldGetter

    override fun visitClassNew(declaration: IrClass): IrStatement {

        if (declaration.isSpecificLoweringLogicApplicable()) {
            handleSpecificNewClass(declaration)
        } else {
            handleNonSpecificNewClass(declaration)
        }

        declaration.transformDeclarationsFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction) {
                withinScope(memberDeclaration) {
                    transformFunctionFlat(memberDeclaration)
                }
            } else {
                memberDeclaration.accept(this, null)
                null
            }
        }

        return declaration
    }

    private fun handleNonSpecificNewClass(declaration: IrClass) {
        declaration.primaryConstructor?.let {
            replacements.getReplacementRegularClassConstructor(it)?.let { replacement -> addBindingsFor(it, replacement) }
        }
        val fieldsToReplace = declaration.fields.filter { !it.type.isNullable() && it.type.isMultiFieldValueClassType() }.toList()

        val oldFieldToInitializers: MutableMap<IrField, IrAnonymousInitializer> = mutableMapOf()
        // we need to preserve order of initializations
        val newFields: List<List<IrField>> = fieldsToReplace.map { oldField ->
            val declarations = replacements.getDeclarations(oldField.type.erasedUpperBound)!!
            val newFields = declarations.fields.map { sourceField ->
                context.irFactory.buildField {
                    name = Name.guessByFirstCharacter("${oldField.name.asString()}$${sourceField.name.asString()}")
                    type = sourceField.type
                    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                    visibility = DescriptorVisibilities.PRIVATE
                }.apply {
                    parent = declaration
                    initializer = null
                }
            }
            regularClassMFVCPropertyFieldsMapping[oldField] = newFields
            val nodes2getters = declarations.makeNodes2FieldExpressions(newFields)

            val property = oldField.correspondingPropertySymbol?.owner
            val mainGetter = property?.getter
            val mainSetter = property?.setter
            property?.backingField = null
            if (mainGetter != null) {
                if (!property.isDelegated && mainGetter.isDefaultGetter(oldField)) {
                    mainGetter.origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                    regularClassMFVCPropertyMainGetters.getOrPut(declaration) { mutableMapOf() }[oldField.name] = mainGetter
                    val nodesToGetters = nodes2getters.mapValues { (node, exprGen) ->
                        if (node == declarations.loweringRepresentation) mainGetter else {
                            context.irFactory.buildProperty {
                                val nameAsString = "${oldField.name.asString()}$${declarations.nodeFullNames[node]!!.asString()}"
                                name = Name.guessByFirstCharacter(nameAsString)
                                origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                                visibility = oldField.visibility
                            }.apply {
                                parent = declaration
                                addGetter {
                                    returnType = node.type
                                    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                                }.apply {
                                    createDispatchReceiverParameter()
                                    val function = this
                                    body = with(context.createIrBuilder(this.symbol)) {
                                        irExprBody(exprGen(this, function))
                                    }
                                }
                            }.getter!!.also {
                                declaration.declarations.add(it)
                            }
                        }
                    }
                    regularClassMFVCPropertyNodes.putAll(nodesToGetters.map { (node, getter) -> getter to node })
                    for ((node, getter) in nodesToGetters) {
                        if (node is InternalNode) {
                            regularClassMFVCPropertyNextGetters[getter] = node.fields.associate { it.name to nodesToGetters[it.node]!! }
                        }
                    }
                    regularClassMFVCPropertyAllPrimitiveGetters.addAll(nodesToGetters.values)
                }
            }
            for (accessor in listOfNotNull(mainGetter, mainSetter))
                accessor.body?.transform(object : IrElementTransformerVoid() {
                    override fun visitGetField(expression: IrGetField): IrExpression {
                        if (expression.symbol.owner == oldField) {
                            require(expression.receiver.let {
                                it is IrGetValue && it.symbol.owner == accessor.dispatchReceiverParameter ||
                                        it == null && oldField.origin == JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD
                            }) { "Unexpected receiver for IrGetField: ${expression.receiver}" }
                            val gettersAndSetters =
                                newFields.toGettersAndSetters(accessor.dispatchReceiverParameter, transformReceiver = true)
                            val representation = newFields.zip(gettersAndSetters) { newField, (getter, setter) ->
                                VirtualProperty(newField.type, getter, setter, null)
                            }
                            val fieldRepresentation = declarations.ImplementationAgnostic(oldField.type as IrSimpleType, representation)
                            val boxed = fieldRepresentation.boxedExpression(
                                context.createIrBuilder((currentScope!!.irElement as IrSymbolOwner).symbol)
                            )
                            valueDeclarationsRemapper.registerExpression(boxed, fieldRepresentation)
                            return boxed
                        }
                        return super.visitGetField(expression)
                    }
                }, null)
            oldField.initializer?.let { initializer ->
                context.irFactory.createAnonymousInitializer(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET, origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER,
                    symbol = IrAnonymousInitializerSymbolImpl()
                ).apply {
                    parent = declaration
                    body = context.createIrBuilder(this.symbol).irBlockBody {
                        +irBlock {
                            flattenExpressionTo(initializer.expression, newFields.toGettersAndSetters(declaration.thisReceiver))
                        }
                    }
                    oldFieldToInitializers[oldField] = this
                }
            }
            newFields
        }
        for (i in declaration.declarations.indices) {
            oldFieldToInitializers[declaration.declarations[i]]?.let { initializer ->
                declaration.declarations[i] = initializer
            }
        }
        declaration.declarations.addAll(newFields.flatten())
        declaration.declarations.removeAll(fieldsToReplace)
    }

    override fun handleSpecificNewClass(declaration: IrClass) {
        replacements.commitMFVCOldProperties(declaration)
        val newDeclarations = replacements.getDeclarations(declaration)!!
        if (newDeclarations.valueClass != declaration) error("Unexpected IrClass ${newDeclarations.valueClass} instead of $declaration")
        newDeclarations.replaceFields()
        newDeclarations.replaceProperties()
        newDeclarations.buildPrimaryMultiFieldValueClassConstructor()
        newDeclarations.buildBoxFunction()
        newDeclarations.buildUnboxFunctions()
        newDeclarations.buildSpecializedEqualsMethod()
    }

    override fun transformSecondaryConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.valueParameters.forEach { it.transformChildrenVoid() }
        replacement.body = context.createIrBuilder(replacement.symbol).irBlockBody {
            val thisVar = irTemporary(irType = replacement.returnType, nameHint = "\$this")
            constructor.body?.statements?.forEach { statement ->
                +statement.transformStatement(object : IrElementTransformerVoid() {
                    override fun visitClass(declaration: IrClass): IrStatement = declaration

                    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                        val oldPrimaryConstructor = replacements.getDeclarations(constructor.constructedClass)!!.oldPrimaryConstructor
                        thisVar.initializer = irCall(oldPrimaryConstructor).apply {
                            copyTypeAndValueArgumentsFrom(expression)
                        }
                        return irBlock {}
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression = when (expression.symbol.owner) {
                        constructor.constructedClass.thisReceiver!! -> irGet(thisVar)
                        else -> super.visitGetValue(expression)
                    }

                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid()
                        if (expression.returnTargetSymbol != constructor.symbol)
                            return expression

                        return irReturn(irBlock(expression.startOffset, expression.endOffset) {
                            +expression.value
                            +irGet(thisVar)
                        })
                    }
                })
            }
            +irReturn(irGet(thisVar))
        }
            .also { addBindingsFor(constructor, replacement) }
            .transform(this@JvmMultiFieldValueClassLowering, null)
            .patchDeclarationParents(replacement)
        return listOf(replacement)
    }

    private fun MultiFieldValueClassSpecificDeclarations.replaceFields() {
        valueClass.declarations.removeIf { it is IrField }
        valueClass.declarations += fields
    }

    private fun MultiFieldValueClassSpecificDeclarations.replaceProperties() {
        valueClass.declarations.removeAll(oldProperties.values.mapNotNull { it.getter })
        valueClass.declarations += properties.values.map { it.getter!! }
    }

    override fun createBridgeDeclaration(source: IrSimpleFunction, replacement: IrSimpleFunction, mangledName: Name): IrSimpleFunction =
        context.irFactory.buildFun {
            updateFrom(source)
            name = mangledName
            returnType = source.returnType
        }.apply {
            val anyOverriddenReplaced = replacement.overriddenSymbols.any {
                it.owner in replacements.bindingNewFunctionToParameterTemplateStructure
            }
            if (source.parentAsClass.isMultiFieldValueClass && anyOverriddenReplaced) {
                copyTypeParametersFrom(source) // without static type parameters
                val substitutionMap = makeTypeParameterSubstitutionMap(source, this)
                dispatchReceiverParameter = source.dispatchReceiverParameter!!.let { // source!!!
                    it.copyTo(this, type = it.type.substitute(substitutionMap))
                }
                require(replacement.dispatchReceiverParameter == null) {
                    """
                        Ambiguous receivers:
                        ${source.dispatchReceiverParameter!!.render()}
                        ${replacement.dispatchReceiverParameter!!.render()}
                        """.trimIndent()
                }
                require(replacement.extensionReceiverParameter == null) {
                    "Static replacement must have no extension receiver but ${replacement.extensionReceiverParameter!!.render()} found"
                }
                val replacementStructure = replacements.bindingNewFunctionToParameterTemplateStructure[replacement]!!
                val offset = replacementStructure[0].valueParameters.size
                valueParameters = replacement.valueParameters.drop(offset).map {
                    it.copyTo(this, type = it.type.substitute(substitutionMap), index = it.index - offset)
                }
                val bridgeStructure =
                    replacementStructure.toMutableList().apply {
                        set(0, RegularMapping(ValueParameterTemplate(dispatchReceiverParameter!!, dispatchReceiverParameter!!.origin)))
                    }
                replacements.bindingNewFunctionToParameterTemplateStructure[this] = bridgeStructure
            } else {
                copyParameterDeclarationsFrom(source)
            }
            annotations = source.annotations
            parent = source.parent
            // We need to ensure that this bridge has the same attribute owner as its static inline class replacement, since this
            // is used in [CoroutineCodegen.isStaticInlineClassReplacementDelegatingCall] to identify the bridge and avoid generating
            // a continuation class.
            copyAttributes(source)
        }

    override fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction, original: IrFunction, inverted: Boolean) {

        allScopes.push(createScope(source))
        source.body = context.createIrBuilder(source.symbol, source.startOffset, source.endOffset).run {
            val sourceExplicitParameters = source.explicitParameters
            val targetExplicitParameters = target.explicitParameters
            irExprBody(irCall(target).apply {
                passTypeArgumentsWithOffsets(target, source) { source.typeParameters[it].defaultType }
                val sourceStructure: List<RemappedParameter>? = replacements.bindingNewFunctionToParameterTemplateStructure[source]
                val targetStructure: List<RemappedParameter>? = replacements.bindingNewFunctionToParameterTemplateStructure[target]
                val errorMessage = {
                    """
                        Incompatible structures for
                        Source: $sourceStructure
                        ${source.render()}
                        Target: $targetStructure
                        ${target.render()}
                        """.trimIndent()
                }
                when (targetStructure) {
                    null -> when (sourceStructure) {
                        null -> require(targetExplicitParameters.size == sourceExplicitParameters.size, errorMessage)
                        else -> {
                            require(targetExplicitParameters.size == sourceStructure.size, errorMessage)
                            require(sourceExplicitParameters.size == sourceStructure.sumOf { it.valueParameters.size }, errorMessage)
                        }
                    }
                    else -> when (sourceStructure) {
                        null -> {
                            require(targetStructure.size == sourceExplicitParameters.size, errorMessage)
                            require(targetStructure.sumOf { it.valueParameters.size } == targetExplicitParameters.size, errorMessage)
                        }
                        else -> {
                            require(targetStructure.size == sourceStructure.size, errorMessage)
                            require(sourceStructure.sumOf { it.valueParameters.size } == sourceExplicitParameters.size, errorMessage)
                            require(targetStructure.sumOf { it.valueParameters.size } == targetExplicitParameters.size, errorMessage)
                            require((targetStructure zip sourceStructure).none { (t, s) ->
                                t is MultiFieldValueClassMapping && s is MultiFieldValueClassMapping && t.declarations != s.declarations
                            }, errorMessage)
                        }
                    }
                }
                val structuresSizes = sourceStructure?.size ?: targetStructure?.size ?: targetExplicitParameters.size
                var flattenedSourceIndex = 0
                var flattenedTargetIndex = 0
                for (i in 0 until structuresSizes) {
                    val remappedSourceParameter = sourceStructure?.get(i)
                    val remappedTargetParameter = targetStructure?.get(i)
                    when (remappedSourceParameter) {
                        is MultiFieldValueClassMapping -> {
                            when (remappedTargetParameter) {
                                is MultiFieldValueClassMapping -> {
                                    require(remappedTargetParameter.valueParameters.size == remappedSourceParameter.valueParameters.size) {
                                        "Incompatible structures: $remappedTargetParameter, $remappedSourceParameter"
                                    }
                                    repeat(remappedTargetParameter.valueParameters.size) {
                                        putArgument(
                                            targetExplicitParameters[flattenedTargetIndex++],
                                            irGet(sourceExplicitParameters[flattenedSourceIndex++])
                                        )
                                    }
                                }
                                is RegularMapping, null -> putArgument(
                                    targetExplicitParameters[flattenedTargetIndex++],
                                    irCall(remappedSourceParameter.declarations.boxMethod).apply {
                                        remappedSourceParameter.boxedType.arguments.forEachIndexed { index, argument ->
                                            putTypeArgument(index, argument.typeOrNull)
                                        }
                                        sourceExplicitParameters
                                            .slice(flattenedSourceIndex until flattenedSourceIndex + remappedSourceParameter.valueParameters.size)
                                            .forEachIndexed { index, boxParameter -> putValueArgument(index, irGet(boxParameter)) }
                                            .also { flattenedSourceIndex += remappedSourceParameter.valueParameters.size }
                                    })
                            }
                        }
                        is RegularMapping, null -> when (remappedTargetParameter) {
                            is MultiFieldValueClassMapping -> {
                                val receiver = sourceExplicitParameters[flattenedSourceIndex++]
                                for (unboxMethod in remappedTargetParameter.declarations.unboxMethods) {
                                    putArgument(
                                        targetExplicitParameters[flattenedTargetIndex++],
                                        irCall(unboxMethod).apply { dispatchReceiver = irGet(receiver) }
                                    )
                                }
                            }
                            else -> putArgument(
                                targetExplicitParameters[flattenedTargetIndex++],
                                irGet(sourceExplicitParameters[flattenedSourceIndex++])
                            )
                        }
                    }
                }
                require(flattenedTargetIndex == targetExplicitParameters.size && flattenedSourceIndex == sourceExplicitParameters.size) {
                    "Incorrect source:\n${source.dump()}\n\nfor target\n${target.dump()}"
                }
            })
        }
        allScopes.pop()
    }

    private fun IrFunctionAccessExpression.passTypeArgumentsWithOffsets(
        target: IrFunction, source: IrFunction, forCommonTypeParameters: (targetIndex: Int) -> IrType
    ) {
        val passedTypeParametersSize = minOf(target.typeParameters.size, source.typeParameters.size)
        val targetOffset = target.typeParameters.size - passedTypeParametersSize
        val sourceOffset = source.typeParameters.size - passedTypeParametersSize
        if (sourceOffset > 0) {
            // static fun calls method
            val dispatchReceiverType = source.parentAsClass.defaultType
            require(dispatchReceiverType.let {
                it.isMultiFieldValueClassType() && !it.isNullable() && it.erasedUpperBound.typeParameters.size == sourceOffset
            }) { "Unexpected dispatcher receiver type: ${dispatchReceiverType.render()}" }
        }
        if (targetOffset > 0) {
            // method calls static fun
            val dispatchReceiverType = source.parentAsClass.defaultType
            require(dispatchReceiverType.let {
                it.isMultiFieldValueClassType() && !it.isNullable() && it.erasedUpperBound.typeParameters.size == targetOffset
            }) { "Unexpected dispatcher receiver type: ${dispatchReceiverType.render()}" }
            dispatchReceiverType.erasedUpperBound.typeParameters.forEachIndexed { index, typeParameter ->
                putTypeArgument(index, typeParameter.defaultType)
            }
        }
        for (i in 0 until passedTypeParametersSize) {
            putTypeArgument(i + targetOffset, forCommonTypeParameters(i + sourceOffset))
        }
    }

    override fun addBindingsFor(original: IrFunction, replacement: IrFunction) {
        val parametersStructure = replacements.bindingOldFunctionToParameterTemplateStructure[original]!!
        require(parametersStructure.size == original.explicitParameters.size) {
            "Wrong value parameters structure: $parametersStructure"
        }
        require(parametersStructure.sumOf { it.valueParameters.size } == replacement.explicitParameters.size) {
            "Wrong value parameters structure: $parametersStructure"
        }
        val old2newList = original.explicitParameters.zip(
            parametersStructure.scan(0) { partial: Int, templates: RemappedParameter -> partial + templates.valueParameters.size }
                .zipWithNext { start: Int, finish: Int -> replacement.explicitParameters.slice(start until finish) }
        )
        for (i in old2newList.indices) {
            val (param, newParamList) = old2newList[i]
            when (val structure = parametersStructure[i]) {
                is MultiFieldValueClassMapping ->
                    valueDeclarationsRemapper.remapSymbol(param.symbol, newParamList.map { VirtualProperty(it) }, structure.declarations)
                is RegularMapping -> valueDeclarationsRemapper.remapSymbol(param.symbol, newParamList.single())
            }
        }
    }

    fun MultiFieldValueClassSpecificDeclarations.buildPrimaryMultiFieldValueClassConstructor() {
        valueClass.declarations.removeIf { it is IrConstructor && it.isPrimary }
        val primaryConstructorReplacements = listOf(primaryConstructor, primaryConstructorImpl)
        valueClass.declarations += primaryConstructorReplacements

        val initializersStatements = valueClass.declarations.filterIsInstance<IrAnonymousInitializer>().flatMap { it.body.statements }
        valueDeclarationsRemapper.remapSymbol(
            oldPrimaryConstructor.constructedClass.thisReceiver!!.symbol,
            primaryConstructorImpl.valueParameters.map { VirtualProperty(it) },
            this,
        )
        primaryConstructorImpl.body = context.createIrBuilder(primaryConstructorImpl.symbol).irBlockBody {
            for (stmt in initializersStatements) {
                +stmt.transformStatement(this@JvmMultiFieldValueClassLowering).patchDeclarationParents(primaryConstructorImpl)
            }
        }
        valueClass.declarations.removeIf { it is IrAnonymousInitializer }
    }

    fun MultiFieldValueClassSpecificDeclarations.buildBoxFunction() {
        boxMethod.body = with(context.createIrBuilder(boxMethod.symbol)) {
            irExprBody(irCall(primaryConstructor.symbol).apply {
                for (i in leaves.indices) {
                    putValueArgument(i, irGet(boxMethod.valueParameters[i]))
                }
            })
        }
        valueClass.declarations += boxMethod
    }

    fun MultiFieldValueClassSpecificDeclarations.buildUnboxFunctions() {
        valueClass.declarations += unboxMethods
    }

    @Suppress("unused")
    fun MultiFieldValueClassSpecificDeclarations.buildSpecializedEqualsMethod() {
        // todo defaults
        specializedEqualsMethod.body = with(context.createIrBuilder(specializedEqualsMethod.symbol)) {
            // TODO: Revisit this once we allow user defined equals methods in inline/multi-field value classes.
            leaves.indices.map {
                val left = irGet(specializedEqualsMethod.valueParameters[it])
                val right = irGet(specializedEqualsMethod.valueParameters[it + leaves.size])
                irEquals(left, right)
            }.reduce { acc, current ->
                irCall(context.irBuiltIns.andandSymbol).apply {
                    putValueArgument(0, acc)
                    putValueArgument(1, current)
                }
            }.let { irExprBody(it) }
        }
        valueClass.declarations += specializedEqualsMethod
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        // todo implement
        return super.visitFunctionReference(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner
        val replacement = replacements.getReplacementFunction(function)
        val currentScope = currentScope!!.irElement as IrDeclaration
        return when {
            function is IrConstructor && function.isPrimary && function.constructedClass.isMultiFieldValueClass &&
                    currentScope.origin != JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER -> {
                context.createIrBuilder(currentScope.symbol).irBlock {
                    val thisReplacement = irTemporary(expression)
                    +irGet(thisReplacement)
                }.transform(this, null) // transform with visitVariable
            }
            replacement != null -> context.createIrBuilder(currentScope.symbol).irBlock {
                buildReplacement(function, expression, replacement)
            }
            else ->
                when (val newConstructor = (function as? IrConstructor)?.let { replacements.getReplacementRegularClassConstructor(it) }) {
                    null -> return super.visitFunctionAccess(expression)
                    else -> context.createIrBuilder(currentScope.symbol).irBlock {
                        buildReplacement(function, expression, newConstructor)
                    }
                }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        val oldReceiver = expression.dispatchReceiver
        val name = callee.correspondingPropertySymbol?.owner?.name
        if (callee.isMultiFieldValueClassOriginalFieldGetter && oldReceiver != null) {
            with(valueDeclarationsRemapper) {
                with(context.createIrBuilder(expression.symbol)) {
                    val newReceiver = oldReceiver.transform(this@JvmMultiFieldValueClassLowering, null)
                    if (newReceiver is IrCall) {
                        val nextFunction = regularClassMFVCPropertyNextGetters[newReceiver.symbol.owner]?.get(name)
                        if (nextFunction != null) {
                            require(nextFunction.typeParameters.isEmpty()) { "Subgetter with type parameters: ${nextFunction.render()}" }
                            return irCall(nextFunction).apply {
                                this.dispatchReceiver = newReceiver.dispatchReceiver
                            }
                        }
                    }
                    subfield(newReceiver, name!!)?.let { return it }
                    return expression.apply {
                        dispatchReceiver = newReceiver
                        extensionReceiver = extensionReceiver?.transform(this@JvmMultiFieldValueClassLowering, null)
                        for (i in 0 until valueArgumentsCount) {
                            putValueArgument(i, getValueArgument(i)?.transform(this@JvmMultiFieldValueClassLowering, null))
                        }
                    }
                }
            }
        }
        if (expression.isSpecializedMFVCEqEq) {
            val leftArgument = expression.getValueArgument(0)!!.transform(this, null)
            val rightArgument = expression.getValueArgument(1)!!.transform(this, null)
            val leftImplementation = valueDeclarationsRemapper.implementationAgnostic(leftArgument)
            val rightImplementation = valueDeclarationsRemapper.implementationAgnostic(rightArgument)
            if (leftImplementation != null) {
                val leftClass = leftImplementation.regularDeclarations.valueClass
                if (rightImplementation != null) {
                    val rightClass = rightImplementation.regularDeclarations.valueClass
                    require(leftClass == rightClass) { "Equals for different classes: $leftClass and $rightClass called" }
                    return context.createIrBuilder(expression.symbol).run {
                        irCall(leftImplementation.regularDeclarations.specializedEqualsMethod).apply {
                            (leftImplementation.type.arguments + rightImplementation.type.arguments).forEachIndexed { index, argument ->
                                putTypeArgument(index, argument.typeOrNull)
                            }
                            val arguments =
                                (leftImplementation.virtualFields + rightImplementation.virtualFields).map { it.makeGetter(this@run) }
                            arguments.forEachIndexed { index, argument -> putValueArgument(index, argument) }
                        }
                    }
                } else {
                    val equals = leftClass.functions.single { it.name.asString() == "equals" && it.overriddenSymbols.isNotEmpty() }
                    return super.visitCall(context.createIrBuilder(expression.symbol).run {
                        irCall(equals).apply {
                            copyTypeArgumentsFrom(expression)
                            dispatchReceiver = leftArgument
                            putValueArgument(0, rightArgument)
                        } as IrCall
                    })
                }
            } else if (rightImplementation != null) {
                if (leftArgument.isNullConst()) {
                    return context.createIrBuilder(expression.symbol).irBlock {
                        +rightArgument
                        +irFalse()
                    }.transform(this, null)
                }
                if (leftArgument.type.erasedUpperBound == rightArgument.type.erasedUpperBound && leftArgument.type.isNullable()) {
                    return context.createIrBuilder(expression.symbol).irBlock {
                        val leftValue = irTemporary(leftArgument)
                        +irIfNull(context.irBuiltIns.booleanType, irGet(leftValue), irFalse(), irBlock {
                            val nonNullLeftArgumentVariable =
                                irTemporary(irImplicitCast(irGet(leftValue), leftArgument.type.makeNotNull()))
                            +irCall(context.irBuiltIns.eqeqSymbol).apply {
                                copyTypeArgumentsFrom(expression)
                                putValueArgument(0, irGet(nonNullLeftArgumentVariable))
                                putValueArgument(1, rightArgument)
                            }
                        })
                    }.transform(this@JvmMultiFieldValueClassLowering, null)
                }
            }
        }
        return super.visitCall(expression)
    }

    private fun makeLeavesGetters(currentGetter: IrSimpleFunction): List<IrSimpleFunction>? =
        when (val node = regularClassMFVCPropertyNodes[currentGetter]) {
            null -> null
            is Leaf -> listOf(currentGetter)
            is InternalNode -> node.fields.flatMap { makeLeavesGetters(regularClassMFVCPropertyNextGetters[currentGetter]!![it.name]!!)!! }
        }

    private fun IrBlockBuilder.buildReplacement(
        originalFunction: IrFunction,
        original: IrMemberAccessExpression<*>,
        replacement: IrFunction
    ) {
        val parameter2expression = typedArgumentList(originalFunction, original)
        val structure = replacements.bindingOldFunctionToParameterTemplateStructure[originalFunction]!!
        require(parameter2expression.size == structure.size)
        require(structure.sumOf { it.valueParameters.size } == replacement.explicitParametersCount)
        val newArguments: List<IrExpression?> =
            makeNewArguments(parameter2expression.map { (_, argument) -> argument }, structure.map { it.valueParameters })
        +irCall(replacement.symbol).apply {
            passTypeArgumentsWithOffsets(replacement, originalFunction) { original.getTypeArgument(it)!! }
            for ((parameter, argument) in replacement.explicitParameters zip newArguments) {
                if (argument == null) continue
                putArgument(replacement, parameter, argument.transform(this@JvmMultiFieldValueClassLowering, null))
            }
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        for (i in expression.arguments.indices) {
            val argument = expression.arguments[i]
            if (!argument.type.isNullable() && argument.type.isMultiFieldValueClassType()) {
                expression.arguments[i] = context.createIrBuilder((currentScope!!.irElement as IrSymbolOwner).symbol).run {
                    val toString = argument.type.erasedUpperBound.functions.single {
                        it.name.asString() == "toString" && it.valueParameters.isEmpty()
                    }
                    require(toString.typeParameters.isEmpty()) { "Bad toString: ${toString.render()}" }
                    irCall(toString).apply {
                        dispatchReceiver = argument
                    }
                }
            }
        }
        return super.visitStringConcatenation(expression)
    }

    private fun IrBlockBuilder.makeNewArguments(
        oldArguments: List<IrExpression?>,
        structure: List<List<ValueParameterTemplate>>
    ): List<IrExpression?> {

        var variables: List<IrVariable> = listOf()
        var subVariables: List<List<IrVariable>> = listOf()
        val declareVariables = irComposite {
            variables = structure.flatMap { argTemplate -> argTemplate.map { irTemporary(irType = it.type) } }
            subVariables = structure.scan(0) { acc: Int, templates: List<ValueParameterTemplate> -> acc + templates.size }
                .zipWithNext().map { (start, finish) -> variables.slice(start until finish) }
        }
        val initializeVariables = irBlock {
            (oldArguments zip subVariables).forEach { (oldArgument, curSubVariables) ->
                when {
                    oldArgument == null -> Unit
                    curSubVariables.size == 1 -> {
                        val curSubVariable = curSubVariables.single()
                        +irSet(curSubVariable, oldArgument.transform(this@JvmMultiFieldValueClassLowering, null))
                    }
                    else -> flattenExpressionTo(oldArgument, curSubVariables.toGettersAndSetters())
                }
            }
        }
        val variablesSymbols2Index = variables.mapIndexed { index, variable -> variable.symbol to index }.toMap()
        val inlinedArguments = run {
            val result = mutableMapOf<IrValueSymbol, IrExpression>()
            var lastIndex = -1 // we need to assign them in the same order
            for (statement in initializeVariables.statements) {
                if (statement !is IrSetValue) return@run null
                val index = variablesSymbols2Index[statement.symbol] ?: return@run null
                if (lastIndex >= index) return@run null
                lastIndex = index
                result[statement.symbol] = statement.value
            }
            result
        }
        if (inlinedArguments != null) {
            return variables.map { inlinedArguments[it.symbol] }
        }
        +declareVariables
        +initializeVariables
        val newArguments: List<IrGetValueImpl?> = (oldArguments zip subVariables).flatMap { (oldArgument, curSubVariables) ->
            when (oldArgument) {
                null -> List(curSubVariables.size) { null }
                else -> curSubVariables.map { irGet(it) }
            }
        }
        return newArguments
    }

    // Note that reference equality (x === y) is not allowed on values of MFVC class type,
    // so it is enough to check for eqeq.
    private val IrCall.isSpecializedMFVCEqEq: Boolean
        get() = symbol == context.irBuiltIns.eqeqSymbol &&
                listOf(getValueArgument(0)!!, getValueArgument(1)!!)
                    .any { it.type.erasedUpperBound.takeIf { it.isMultiFieldValueClass } != null }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val field = expression.symbol.owner
        val parent = field.parent
        return when {
            field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD &&
                    parent is IrClass &&
                    parent.multiFieldValueClassRepresentation?.containsPropertyWithName(field.name) == true -> {
                val receiver = expression.receiver?.transform(this, null)
                with(valueDeclarationsRemapper) {
                    with(context.createIrBuilder(expression.symbol)) {
                        receiver?.let { subfield(it, field.name) } ?: run {
                            expression.receiver = receiver
                            expression
                        }
                    }
                }
            }
            field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD && !field.type.isNullable() && field.type.isMultiFieldValueClassType() -> {
                val getter = regularClassMFVCPropertyMainGetters[expression.receiver!!.type.erasedUpperBound]?.get(field.name)
                    ?: return super.visitGetField(expression)
                with(context.createIrBuilder(expression.symbol)) {
                    irCall(getter).apply {
                        require(getter.typeParameters.isEmpty()) { "Getter with type parameters: ${getter.render()}" }
                        dispatchReceiver = expression.receiver!!.transform(this@JvmMultiFieldValueClassLowering, null)
                    }
                }
            }
            else -> super.visitGetField(expression)
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val field = expression.symbol.owner
        val replacementFields = regularClassMFVCPropertyFieldsMapping[field] ?: return super.visitSetField(expression)
        return context.createIrBuilder(expression.symbol).irBlock {
            val thisVar = expression.receiver?.let { irTemporary(it.transform(this@JvmMultiFieldValueClassLowering, null)) }
            // We flatten to temp variables because code can throw an exception otherwise and partially update variables
            val subValues = flattenExpressionToGetters(
                expression = expression.value, // not modified
                types = replacementFields.map { it.type },
                additionalConditionForValue = { (_, value) -> value is IrGetValue }
            )
            for ((replacementField, subValue) in replacementFields zip subValues) {
                +irSetField(thisVar?.let { irGet(it) }, replacementField, subValue)
            }
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression = with(context.createIrBuilder(expression.symbol)) {
        with(valueDeclarationsRemapper) {
            getter(expression.symbol) ?: super.visitGetValue(expression)
        }
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        val setters = valueDeclarationsRemapper.setter(expression.symbol) ?: return super.visitSetValue(expression)
        return context.createIrBuilder(expression.symbol).irBlock {
            val declarations = replacements.getDeclarations(expression.symbol.owner.type.erasedUpperBound)!!
            // We flatten to temp variables because code can throw an exception otherwise and partially update variables
            val modifiedExpression = expression.value.transform(this@JvmMultiFieldValueClassLowering, null)
            val currentDeclarationReplacementSymbols = valueDeclarationsRemapper.implementationAgnostic(modifiedExpression)?.symbols
                ?.mapIndexedNotNull { index, symbol -> symbol?.to(index) }?.toMap() ?: emptyMap()
            val subValues = flattenExpressionToGetters(
                expression = expression.value, // not modified
                types = declarations.leaves.map { it.type },
                additionalConditionForValue = { (index, value) ->
                    value is IrGetValue && currentDeclarationReplacementSymbols[value.symbol].let { it == null || it <= index }
                }
            )
            for ((setter, subValue) in setters zip subValues) {
                setter?.invoke(this, subValue)?.let { +it }
            }
        }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val initializer = declaration.initializer
        if (declaration.type.isMultiFieldValueClassType() && !declaration.type.isNullable()) {
            val irClass = declaration.type.erasedUpperBound
            val declarations = replacements.getDeclarations(irClass)!!
            return context.createIrBuilder((currentScope!!.irElement as IrSymbolOwner).symbol).irComposite {
                val variables = declarations.leaves.map { leaf ->
                    irTemporary(
                        nameHint = "${declaration.name.asString()}$${declarations.nodeFullNames[leaf]!!.asString()}",
                        irType = leaf.type,
                        isMutable = declaration.isVar
                    )
                }
                initializer?.let {
                    flattenExpressionTo(it, variables.toGettersAndSetters())
                }
                valueDeclarationsRemapper.remapSymbol(declaration.symbol, variables.map { VirtualProperty(it) }, declarations)
            }
        }
        return super.visitVariable(declaration)
    }

    private fun List<IrVariable>.toGettersAndSetters() = map { variable ->
        Pair<ExpressionGenerator, ExpressionSupplier>(
            { irGet(variable) },
            { value: IrExpression -> irSet(variable, value) }
        )
    }

    private fun List<IrField>.toGettersAndSetters(receiver: IrValueParameter?, transformReceiver: Boolean = false) = map { field ->
        Pair<ExpressionGenerator, ExpressionSupplier>(
            {
                val initialGetReceiver = receiver?.let { irGet(it) }
                val resultReceiver =
                    if (transformReceiver) initialGetReceiver?.transform(this@JvmMultiFieldValueClassLowering, null)
                    else initialGetReceiver
                irGetField(resultReceiver, field)
            },
            { value: IrExpression ->
                val initialGetReceiver = receiver?.let { irGet(it) }
                val resultReceiver =
                    if (transformReceiver) initialGetReceiver?.transform(this@JvmMultiFieldValueClassLowering, null)
                    else initialGetReceiver
                irSetField(resultReceiver, field, value)
            },
        )
    }

    // expression takes not transformed first argument
    private fun IrBlockBuilder.flattenExpressionToGetters(
        expression: IrExpression,
        types: List<IrType>,
        additionalConditionForValue: (IndexedValue<IrExpression>) -> Boolean,
    ): List<IrExpression> {
        val variables = types.map { irTemporary(irType = it) }
        val temporaryBlock = irBlock {
            flattenExpressionTo(expression, variables.toGettersAndSetters())
        }
        val satisfyingSetVariableSymbols = temporaryBlock.statements.mapIndexed { index, blockStatement ->
            (blockStatement as? IrSetValue)?.takeIf { additionalConditionForValue(IndexedValue(index, it.value)) }?.symbol
        }
        return if (variables.map { it.symbol } == satisfyingSetVariableSymbols) {
            temporaryBlock.statements.map { (it as IrSetValue).value }
        } else {
            +temporaryBlock
            variables.map { irGet(it) }
        }
    }

    fun IrBlockBuilder.flattenExpressionTo(
        expression: IrExpression, variables: List<Pair<ExpressionGenerator, ExpressionSupplier>>
    ) {
        val declarations = replacements.getDeclarations(expression.type.erasedUpperBound) ?: run {
            val constructor = (expression as? IrConstructorCall)?.symbol?.owner ?: return@run null
            replacements.getDeclarations(constructor.constructedClass)
        }

        if (expression.type.isNullable() || declarations == null) {
            require(variables.size == 1)
            +variables.single().second(this, expression.transform(this@JvmMultiFieldValueClassLowering, null))
            return
        }
        require(variables.size == declarations.leaves.size)
        if (expression is IrConstructorCall) {
            val constructor = expression.symbol.owner
            if (constructor.isPrimary && constructor.constructedClass.isMultiFieldValueClass) {
                val oldArguments = List(expression.valueArgumentsCount) { expression.getValueArgument(it) }
                val root = declarations.loweringRepresentation
                require(root.fields.size == oldArguments.size) {
                    "$constructor must have ${root.fields.size} arguments but got ${oldArguments.size}"
                }
                var curOffset = 0
                for ((treeField, argument) in root.fields zip oldArguments) {
                    val size = when (treeField.node) {
                        is InternalNode -> replacements.getDeclarations(treeField.node.irClass)!!.leaves.size
                        is Leaf -> 1
                    }
                    val subVariables = variables.slice(curOffset until (curOffset + size)).also { curOffset += size }
                    argument?.let { flattenExpressionTo(it, subVariables) }
                }
                +irCall(declarations.primaryConstructorImpl).apply {
                    copyTypeArgumentsFrom(expression)
                    variables.forEachIndexed { index, variable -> putValueArgument(index, variable.first(this@flattenExpressionTo)) }
                }
                return
            }
        }
        val transformedExpression = expression.transform(this@JvmMultiFieldValueClassLowering, null)
        valueDeclarationsRemapper.implementationAgnostic(transformedExpression)?.virtualFields?.map { it.makeGetter(this) }?.let {
            require(variables.size == it.size)
            for ((variable, subExpression) in variables zip it) {
                +variable.second(this, subExpression)
            }
            return
        }
        if (transformedExpression is IrCall) {
            val callee = transformedExpression.symbol.owner
            if (callee == declarations.boxMethod) {
                require(transformedExpression.valueArgumentsCount == declarations.fields.size) {
                    "Bad arguments number for box-method: ${transformedExpression.valueArgumentsCount}"
                }
                for ((variable, argument) in variables zip List(transformedExpression.valueArgumentsCount) {
                    transformedExpression.getValueArgument(it)
                }) {
                    if (argument != null) {
                        +variable.second(this, argument)
                    }
                }
                return
            }
        }
        (transformedExpression as? IrCall)?.let { makeLeavesGetters(it.symbol.owner) }?.let { getters ->
            val receiver = irTemporary(transformedExpression.dispatchReceiver)
            require(getters.size == variables.size) { "Number of getters must be equal to number of variables" }
            for ((variable, getter) in variables zip getters) {
                require(getter.typeParameters.isEmpty()) { "Getter with type parameters: ${getter.render()}" }
                +variable.second(this, irCall(getter).apply { dispatchReceiver = irGet(receiver) })
            }
            return
        }
        if (transformedExpression is IrContainerExpression && transformedExpression.statements.isNotEmpty()) {
            val last = transformedExpression.statements.popLast()
            if (last is IrExpression) {
                transformedExpression.statements.forEach { +it }
                flattenExpressionTo(last, variables)
                return
            }
        }
        val boxed = irTemporary(transformedExpression)
        for ((variable, unboxMethod) in variables zip declarations.unboxMethods) {
            +variable.second(this, irCall(unboxMethod).apply { dispatchReceiver = irGet(boxed) })
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        return super.visitContainerExpression(expression).apply { expression.accept(ExtraBoxesRemover(), false) }
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        return super.visitBlockBody(body).apply { body.accept(ExtraBoxesRemover(), true) }
    }

    private val visitedContainersLastStatement = mutableMapOf<IrStatementContainer, Boolean>()

    private inner class ExtraBoxesRemover : IrElementVisitor<Unit, Boolean> {

        // Removing box-impl's but not getters because they are auto-generated
        private fun IrExpression.isBoxCallStatement() = this is IrCall && valueDeclarationsRemapper.implementationAgnostic(this) != null &&
                List(valueArgumentsCount) { getValueArgument(it) }.all { it == null || it is IrGetField || it is IrGetValue }

        private fun IrStatement.coercionToUnitArgument() =
            (this as? IrTypeOperatorCall)?.takeIf { it.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT }?.argument

        private fun IrStatement.isBoxCallStatement(): Boolean {
            val coercionToUnitArgument = coercionToUnitArgument()
            if (coercionToUnitArgument?.isBoxCallStatement() == true || this is IrExpression && this.isBoxCallStatement()) {
                return true
            }
            if (coercionToUnitArgument is IrStatementContainer) {
                coercionToUnitArgument.accept(this@ExtraBoxesRemover, true)
            }
            return false
        }

        override fun visitElement(element: IrElement, data: Boolean) = Unit

        override fun visitContainerExpression(expression: IrContainerExpression, data: Boolean) {
            visitStatementContainer(expression, data)
        }

        override fun visitBlockBody(body: IrBlockBody, data: Boolean) {
            visitStatementContainer(body, true) // we last body statement is always not used.
        }

        private fun visitStatementContainer(expression: IrStatementContainer, isInnerContainer: Boolean) {
            visitedContainersLastStatement[expression]?.let { visitedLastStatement ->
                if (!visitedLastStatement && isInnerContainer) {
                    visitedContainersLastStatement[expression] = true
                    expression.statements.lastOrNull()?.let {
                        if (it.isBoxCallStatement()) {
                            expression.statements.popLast()
                        } else {
                            it.accept(this, true)
                        }
                    }
                }
                return
            }
            visitedContainersLastStatement[expression] = isInnerContainer
            val statementsToRemove = mutableListOf<IrStatement>()
            for (i in 0 until expression.statements.size) {
                val isInnerStatement = isInnerContainer || i < expression.statements.lastIndex
                if (isInnerStatement && expression.statements[i].isBoxCallStatement()) {
                    statementsToRemove.add(expression.statements[i])
                } else {
                    expression.statements[i].accept(this, isInnerStatement)
                }
            }
            expression.statements.removeAll(statementsToRemove)
        }
    }

    private fun IrSimpleFunction.isDefaultGetter(field: IrField): Boolean =
        ((body?.statements?.singleOrNull() as? IrReturn)?.value as? IrGetField)?.symbol?.owner == field
}
