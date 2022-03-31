/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.ValueParameterTemplate
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassSpecificDeclarations.ImplementationAgnostic
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassSpecificDeclarations.VirtualProperty
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassTree.InternalNode
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isMultiFieldValueClassType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
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
        private val symbol2getter = mutableMapOf<IrValueSymbol, ExpressionGenerator<Unit>>()
        private val symbol2setters = mutableMapOf<IrValueSymbol, List<ExpressionSupplier<Unit>?>>()
        private val knownExpressions = mutableMapOf<IrExpression, ImplementationAgnostic<Unit>>()

        fun remapSymbol(original: IrValueSymbol, replacement: IrValueDeclaration) {
            symbol2getter[original] = { irGet(replacement) }
            symbol2setters[original] = listOf(if (replacement.isAssignable) { _, value -> irSet(replacement, value) } else null)
        }

        fun remapSymbol(original: IrValueSymbol, unboxed: List<VirtualProperty<Unit>>): Unit =
            remapSymbol(original, replacements.getDeclarations(original.owner.type.erasedUpperBound)!!.ImplementationAgnostic(unboxed))

        fun remapSymbol(original: IrValueSymbol, unboxed: ImplementationAgnostic<Unit>) {
            symbol2getter[original] = {
                unboxed.boxedExpression(this, Unit).also { irExpression -> knownExpressions[irExpression] = unboxed }
            }
            symbol2setters[original] = unboxed.virtualFields.map { it.assigner }
        }

        fun IrBuilderWithScope.getter(original: IrValueSymbol): IrExpression? =
            symbol2getter[original]?.invoke(this, Unit)

        fun setter(original: IrValueSymbol): List<ExpressionSupplier<Unit>?>? = symbol2setters[original]

        fun implementationAgnostic(expression: IrExpression): ImplementationAgnostic<Unit>? = knownExpressions[expression]

        fun IrBuilderWithScope.subfield(expression: IrExpression, name: Name): IrExpression? =
            implementationAgnostic(expression)?.get(name)?.let { (expressionGenerator, representation) ->
                val res = expressionGenerator(this, Unit)
                representation?.let { knownExpressions[res] = it }
                res
            }

        /**
         * Register value declaration instead of singular expressions when possible
         */
        fun registerExpression(getter: IrExpression, representation: ImplementationAgnostic<Unit>) {
            knownExpressions[getter] = representation
        }
    }

    private val valueDeclarationsRemapper = ValueDeclarationsRemapper()

    private val regularClassMFVCPropertyMainGetters: MutableMap<IrClass, MutableMap<Name, IrSimpleFunction>> = mutableMapOf()
    private val regularClassMFVCPropertyNextGetters: MutableMap<IrSimpleFunction, Map<Name, IrSimpleFunction>> = mutableMapOf()
    private val regularClassMFVCPropertyFieldsMapping: MutableMap<IrField, List<IrField>> = mutableMapOf()
    private val regularClassMFVCPropertyNodes: MutableMap<IrSimpleFunction, MultiFieldValueClassTree<Unit, Unit>> = mutableMapOf()

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
        // todo test it
        val newFields: List<List<IrField>> = fieldsToReplace.map { oldField ->
            val declarations = replacements.getDeclarations(oldField.type.erasedUpperBound)!!
            val newFields = declarations.fields.map { sourceField ->
                context.irFactory.buildField {
                    name = Name.guessByFirstCharacter("${oldField.name.asString()}$${sourceField.name.asString()}")
                    type = sourceField.type
                    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                    visibility = sourceField.visibility
                }.apply {
                    parent = declaration
                    initializer
                }
            }
            regularClassMFVCPropertyFieldsMapping[oldField] = newFields
            val virtualFields: List<VirtualProperty<IrValueDeclaration>> = newFields.map { newField ->
                VirtualProperty(
                    type = newField.type,
                    makeGetter = { receiver: IrValueDeclaration -> irGetField(irGet(receiver), newField) },
                    assigner = { receiver: IrValueDeclaration, value: IrExpression -> irSetField(irGet(receiver), newField, value) },
                )
            }
            val implementationAgnostic = declarations.ImplementationAgnostic(virtualFields)

            val property = oldField.correspondingPropertySymbol?.owner
            val mainGetter = property?.getter
            val mainSetter = property?.setter
            property?.backingField = null
            if (mainGetter != null) {
                if (!property.isDelegated && mainGetter.isDefaultGetter(oldField)) {
                    mainGetter.origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                    regularClassMFVCPropertyMainGetters.getOrPut(declaration) { mutableMapOf() }[oldField.name] = mainGetter
                    val nodesToGetters = implementationAgnostic.nodeToExpressionGetters.mapValues { (node, exprGen) ->
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
                                    body = with(context.createIrBuilder(this.symbol)) {
                                        irExprBody(exprGen(this, dispatchReceiverParameter!!))
                                    }
                                }
                            }.getter!!.also {
                                declaration.declarations.add(it)
                            }
                        }
                    }
                    regularClassMFVCPropertyNodes.putAll(nodesToGetters.map { (node, getter) -> getter to node })
                    for ((node, getter) in nodesToGetters) {
                        if (node is InternalNode<Unit, Unit>) {
                            regularClassMFVCPropertyNextGetters[getter] = node.fields.associate { it.name to nodesToGetters[it.node]!! }
                        }
                    }
                }
            }
            for (accessor in listOfNotNull(mainGetter, mainSetter))
                accessor.body?.transform(object : IrElementTransformerVoid() {
                    override fun visitGetField(expression: IrGetField): IrExpression {
                        if (expression.symbol.owner == oldField) {
                            require(expression.receiver.let { it is IrGetValue && it.symbol.owner == accessor.dispatchReceiverParameter!! }) {
                                "Unexpected receiver for IrGetField: ${expression.receiver}"
                            }
                            val gettersAndSetters =
                                newFields.toGettersAndSetters(accessor.dispatchReceiverParameter!!, transformReceiver = true)
                            val representation = newFields.zip(gettersAndSetters) { newField, (getter, setter) ->
                                VirtualProperty(newField.type, getter, setter)
                            }
                            val fieldRepresentation = declarations.ImplementationAgnostic(representation)
                            val boxed = fieldRepresentation.boxedExpression(
                                context.createIrBuilder((currentScope!!.irElement as IrSymbolOwner).symbol), Unit
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
                            flattenExpressionTo(
                                initializer.expression.transform(this@JvmMultiFieldValueClassLowering, null),
                                newFields.toGettersAndSetters(declaration.thisReceiver!!)
                            )
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
        replacements.setOldFields(declaration, declaration.fields.toList())
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
        for (field in fields) {
            field.parent = valueClass
        }
    }

    private fun MultiFieldValueClassSpecificDeclarations.replaceProperties() {
        valueClass.declarations.removeIf { it is IrFunction && it.isFieldGetterToRemove() }
        properties.values.forEach {
            it.parent = valueClass
        }
        valueClass.declarations += properties.values.map { it.getter!!.apply { parent = valueClass } }
    }

    override fun createBridgeFunction(function: IrSimpleFunction, replacement: IrSimpleFunction): IrSimpleFunction? {
        return null // todo
        // todo change return type to non-nullable for base class
    }

    override fun addBindingsFor(original: IrFunction, replacement: IrFunction) {
        val parametersStructure = replacements.bindingParameterTemplateStructure[original]!!
        require(parametersStructure.size == original.explicitParameters.size) {
            "Wrong value parameters structure: $parametersStructure"
        }
        require(parametersStructure.sumOf { it.size } == replacement.explicitParameters.size) {
            "Wrong value parameters structure: $parametersStructure"
        }
        val old2newList = original.explicitParameters.zip(
            parametersStructure.scan(0) { partial: Int, templates: List<ValueParameterTemplate> -> partial + templates.size }
                .zipWithNext { start: Int, finish: Int -> replacement.explicitParameters.slice(start until finish) }
        )
        for ((param, newParamList) in old2newList) {
            val single = newParamList.singleOrNull()
            if (single != null) {
                valueDeclarationsRemapper.remapSymbol(param.symbol, single)
            } else {
                valueDeclarationsRemapper.remapSymbol(param.symbol, newParamList.map { VirtualProperty(it) })
            }
        }
    }

    fun MultiFieldValueClassSpecificDeclarations.buildPrimaryMultiFieldValueClassConstructor() {
        valueClass.declarations.removeIf { it is IrConstructor && it.isPrimary }
        val primaryConstructorReplacements = listOf(primaryConstructor, primaryConstructorImpl)
        for (exConstructor in primaryConstructorReplacements) {
            exConstructor.parent = valueClass
        }
        valueClass.declarations += primaryConstructorReplacements

        val initializersStatements = valueClass.declarations.filterIsInstance<IrAnonymousInitializer>().flatMap { it.body.statements }
        valueDeclarationsRemapper.remapSymbol(
            oldPrimaryConstructor.constructedClass.thisReceiver!!.symbol,
            primaryConstructorImpl.valueParameters.map { VirtualProperty(it) }
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
                passTypeArgumentsFrom(boxMethod)
                for (i in leaves.indices) {
                    putValueArgument(i, irGet(boxMethod.valueParameters[i]))
                }
            })
        }
        valueClass.declarations += boxMethod
        boxMethod.parent = valueClass
    }

    fun MultiFieldValueClassSpecificDeclarations.buildUnboxFunctions() {
        valueClass.declarations += unboxMethods
    }

    @Suppress("unused")
    fun MultiFieldValueClassSpecificDeclarations.buildSpecializedEqualsMethod() {
        // todo defaults
        specializedEqualsMethod.parent = valueClass
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
                    null -> super.visitFunctionAccess(expression)
                    else -> context.createIrBuilder(currentScope.symbol).irBlock {
                        buildReplacement(function, expression, newConstructor)
                    }
                }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        val receiver = expression.dispatchReceiver?.transform(this, null)
        val name = callee.correspondingPropertySymbol?.owner?.name
        if (callee.isMultiFieldValueClassOriginalFieldGetter && receiver != null) {
            with(valueDeclarationsRemapper) {
                with(context.createIrBuilder(expression.symbol)) {
                    subfield(receiver, name!!)?.let { return it }
                }
            }
        }
        if (callee.isGetter && name != null && receiver is IrCall) {
            val nextFunction = regularClassMFVCPropertyNextGetters[receiver.symbol.owner]?.get(name)
            if (nextFunction != null) {
                return with(context.createIrBuilder(expression.symbol)) {
                    irCall(nextFunction).apply {
                        this.dispatchReceiver = receiver.dispatchReceiver
                    }
                }
            }
        }
        @Suppress("ControlFlowWithEmptyBody")
        if (expression.isSpecializedInlineClassEqEq) {
            // todo
        }
        return super.visitCall(expression)
    }

    private fun makeLeavesGetters(currentGetter: IrSimpleFunction): List<IrSimpleFunction>? =
        when (val node = regularClassMFVCPropertyNodes[currentGetter]) {
            null -> null
            is MultiFieldValueClassTree.Leaf<Unit> -> listOf(currentGetter)
            is InternalNode -> node.fields.flatMap { makeLeavesGetters(regularClassMFVCPropertyNextGetters[currentGetter]!![it.name]!!)!! }
        }

    private fun IrBlockBuilder.buildReplacement(
        originalFunction: IrFunction,
        original: IrMemberAccessExpression<*>,
        replacement: IrFunction
    ) {
        val parameter2expression = typedArgumentList(originalFunction, original)
        val structure = replacements.bindingParameterTemplateStructure[originalFunction]!!
        require(parameter2expression.size == structure.size)
        require(structure.sumOf { it.size } == replacement.explicitParametersCount)
        val newArguments: List<IrExpression?> = makeNewArguments(parameter2expression.map { (_, argument) -> argument }, structure)
        +irCall(replacement.symbol).apply {
            copyTypeArgumentsFrom(original)
            for ((parameter, argument) in replacement.explicitParameters zip newArguments) {
                if (argument == null) continue
                putArgument(replacement, parameter, argument.transform(this@JvmMultiFieldValueClassLowering, null))
            }
        }
    }

    private fun IrBlockBuilder.makeNewArguments(
        oldArguments: List<IrExpression?>,
        structure: List<List<ValueParameterTemplate>>
    ): List<IrExpression?> {
        val variables = structure.flatMap { argTemplate -> argTemplate.map { irTemporary(irType = it.type) } }
        val subVariables = structure.scan(0) { acc: Int, templates: List<ValueParameterTemplate> -> acc + templates.size }
            .zipWithNext().map { (start, finish) -> variables.slice(start until finish) }
        return (oldArguments zip subVariables).flatMap { (oldArgument, curSubVariables) ->
            val oldArgumentTransformed = oldArgument?.transform(this@JvmMultiFieldValueClassLowering, null)
            when {
                oldArgumentTransformed == null -> List(curSubVariables.size) { null }
                curSubVariables.size == 1 -> listOf(oldArgumentTransformed)
                else -> flattenExpressionTo(oldArgumentTransformed, curSubVariables.toGettersAndSetters())
                    .let { curSubVariables.map { irGet(it) } }
            }
        }
    }

    // Note that reference equality (x === y) is not allowed on values of inline class type,
    // so it is enough to check for eqeq.
    private val IrCall.isSpecializedInlineClassEqEq: Boolean
        get() = symbol == context.irBuiltIns.eqeqSymbol &&
                getValueArgument(0)?.type?.classOrNull?.owner?.takeIf { it.isMultiFieldValueClass } != null

    override fun visitGetField(expression: IrGetField): IrExpression {
        val field = expression.symbol.owner
        val parent = field.parent
        return when {
            field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD &&
                    parent is IrClass &&
                    parent.multiFieldValueClassRepresentation?.containsPropertyWithName(field.name) == true -> {
                val receiver = expression.receiver!!.transform(this, null)
                with(valueDeclarationsRemapper) {
                    with(context.createIrBuilder(expression.symbol)) {
                        subfield(receiver, field.name) ?: run {
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
            val thisVar = irTemporary(expression.receiver!!.transform(this@JvmMultiFieldValueClassLowering, null))
            val variables = replacementFields.map { irTemporary(irType = it.type, nameHint = it.name.asString()) }
            // We flatten to temp variables because code can throw an exception otherwise and partially update variables
            flattenExpressionTo(expression.value.transform(this@JvmMultiFieldValueClassLowering, null), variables.toGettersAndSetters())
            for ((replacementField, variable) in replacementFields zip variables) {
                +irSetField(irGet(thisVar), replacementField, irGet(variable))
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
            val variables = declarations.leaves.map { irTemporary(irType = it.type) }
            // We flatten to temp variables because code can throw an exception otherwise and partially update variables
            flattenExpressionTo(expression.value.transform(this@JvmMultiFieldValueClassLowering, null), variables.toGettersAndSetters())
            for ((setter, variable) in setters zip variables) {
                setter?.invoke(this, Unit, irGet(variable))?.let { +it }
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
                valueDeclarationsRemapper.remapSymbol(declaration.symbol, variables.map { VirtualProperty(it) })
            }
        }
        return super.visitVariable(declaration)
    }

    private fun List<IrVariable>.toGettersAndSetters() = map { variable ->
        Pair<ExpressionGenerator<Unit>, ExpressionSupplier<Unit>>(
            { irGet(variable) },
            { _, value: IrExpression -> irSet(variable, value) }
        )
    }

    private fun List<IrField>.toGettersAndSetters(receiver: IrValueParameter, transformReceiver: Boolean = false) = map { field ->
        Pair<ExpressionGenerator<Unit>, ExpressionSupplier<Unit>>(
            {
                val initialGetReceiver = irGet(receiver)
                val resultReceiver =
                    if (transformReceiver) initialGetReceiver.transform(this@JvmMultiFieldValueClassLowering, null)
                    else initialGetReceiver
                irGetField(resultReceiver, field)
            },
            { _, value: IrExpression ->
                val initialGetReceiver = irGet(receiver)
                val resultReceiver =
                    if (transformReceiver) initialGetReceiver.transform(this@JvmMultiFieldValueClassLowering, null)
                    else initialGetReceiver
                irSetField(resultReceiver, field, value)
            },
        )
    }

    fun IrBlockBuilder.flattenExpressionTo(
        expression: IrExpression, variables: List<Pair<ExpressionGenerator<Unit>, ExpressionSupplier<Unit>>>
    ) {
        valueDeclarationsRemapper.implementationAgnostic(expression)?.virtualFields?.map { it.makeGetter(this, Unit) }?.let {
            require(variables.size == it.size)
            for ((variable, subExpression) in variables zip it) {
                +variable.second(this, Unit, subExpression)
            }
            return
        }
        if (expression.type.isNullable() || !expression.type.isMultiFieldValueClassType()) {
            require(variables.size == 1)
            +variables.single().second(this, Unit, expression.transform(this@JvmMultiFieldValueClassLowering, null))
            return
        }
        val declarations = replacements.getDeclarations(expression.type.erasedUpperBound)!!
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
                        is InternalNode -> replacements.getDeclarations(treeField.node.irClass!!)!!.leaves.size
                        is MultiFieldValueClassTree.Leaf -> 1
                    }
                    val subVariables = variables.slice(curOffset until (curOffset + size)).also { curOffset += size }
                    argument?.let { flattenExpressionTo(it, subVariables) } ?: List(size) { null }
                }
                +irCall(declarations.primaryConstructorImpl).apply {
                    variables.forEachIndexed { index, variable -> putValueArgument(index, variable.first(this@flattenExpressionTo, Unit)) }
                }
                return
            }
        }
        val transformedExpression = expression.transform(this@JvmMultiFieldValueClassLowering, null)
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
                        +variable.second(this, Unit, argument)
                    }
                }
                return
            }
        }
        (transformedExpression as? IrCall)?.let { makeLeavesGetters(it.symbol.owner) }?.let { geters ->
            val receiver = irTemporary(transformedExpression.dispatchReceiver)
            require(geters.size == variables.size) { "Number of getters must be equal to number of variables" }
            for ((variable, getter) in variables zip geters) {
                +variable.second(this, Unit, irCall(getter).apply { dispatchReceiver = irGet(receiver) })
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
            +variable.second(this, Unit, irCall(unboxMethod).apply { dispatchReceiver = irGet(boxed) })
        }
    }

    override fun visitStatementContainer(container: IrStatementContainer) {
        super.visitStatementContainer(container)
        deleteUselessBoxes(container)
    }

    private fun deleteUselessBoxes(container: IrStatementContainer) {
        val statements = container.statements

        // Removing box-impl's but not getters because they are auto-generated
        fun IrExpression.isBoxCallStatement() = this is IrCall && valueDeclarationsRemapper.implementationAgnostic(this) != null &&
                List(valueArgumentsCount) { getValueArgument(it) }.all { it == null || it is IrGetField || it is IrGetValue }

        fun IrStatement.coercionToUnitArgument() =
            (this as? IrTypeOperatorCall)?.takeIf { it.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT }?.argument

        fun IrStatement.isBoxCallStatement(): Boolean {
            val coercionToUnitArgument = coercionToUnitArgument()
            if (coercionToUnitArgument?.isBoxCallStatement() == true || this is IrExpression && this.isBoxCallStatement()) {
                return true
            }
            if (coercionToUnitArgument is IrStatementContainer && (coercionToUnitArgument.statements.lastOrNull() as? IrExpression)?.isBoxCallStatement() == true) {
                coercionToUnitArgument.statements.popLast()
            }
            return false
        }

        if (statements.isEmpty()) return

        val last = statements.popLast()

        statements.removeIf { it.isBoxCallStatement() }
        for (statement in statements) {
            if (statement is IrStatementContainer && statement.statements.lastOrNull()?.isBoxCallStatement() == true) {
                statement.statements.removeLast()
            }
        }
        statements.add(last) // we don't check delete it anyway
    }

    private fun IrSimpleFunction.isDefaultGetter(field: IrField): Boolean =
        ((body?.statements?.singleOrNull() as? IrReturn)?.value as? IrGetField)?.symbol?.owner == field
}
