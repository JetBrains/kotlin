/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.RegularMapping
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.lower.BlockOrBody.Block
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

val jvmMultiFieldValueClassPhase = makeIrFilePhase(
    { c: JvmBackendContext ->
        if (c.config.supportMultiFieldValueClasses)
            JvmMultiFieldValueClassLowering(c)
        else
            FileLoweringPass.Empty
    },
    name = "MultiFieldValueClasses",
    description = "Lower multi-field value classes",
)

internal class JvmMultiFieldValueClassLowering(context: JvmBackendContext) : JvmValueClassAbstractLowering(context) {

    private sealed class MfvcNodeInstanceAccessor {
        abstract val instance: MfvcNodeInstance
        abstract operator fun get(name: Name): MfvcNodeInstanceAccessor?
        data class Getter(override val instance: MfvcNodeInstance) : MfvcNodeInstanceAccessor() {
            override operator fun get(name: Name): Getter? = instance[name]?.let { Getter(it) }
        }

        data class Setter(override val instance: MfvcNodeInstance, val values: List<IrExpression>) : MfvcNodeInstanceAccessor() {
            override operator fun get(name: Name): Setter? = instance[name]?.let {
                val indices = (instance as MfvcNodeWithSubnodes).subnodeIndices[it.node]!!
                Setter(it, values.slice(indices))
            }
        }
    }

    private val possibleExtraBoxUsageGenerated = mutableSetOf<IrDeclaration>()

    private val irCurrentScope
        get() = currentScope!!.irElement as IrDeclaration
    private val irCurrentClass
        get() = currentClass!!.irElement as IrClass

    private fun registerPossibleExtraBoxUsage() {
        possibleExtraBoxUsageGenerated.add(irCurrentScope)
    }

    /**
     * The class is used to get replacing expression and MFVC instance if present for the given old value declaration.
     */
    private inner class ValueDeclarationRemapper {

        private val expression2MfvcNodeInstanceAccessor = mutableMapOf<IrExpression, MfvcNodeInstanceAccessor>()
        private val oldSymbol2MfvcNodeInstance = mutableMapOf<IrValueSymbol, ValueDeclarationMfvcNodeInstance>()
        private val oldValueSymbol2NewValueSymbol = mutableMapOf<IrValueSymbol, IrValueSymbol>()

        /**
         * Registers one-to-one replacement
         */
        fun registerReplacement(original: IrValueDeclaration, replacement: IrValueDeclaration) {
            oldValueSymbol2NewValueSymbol[original.symbol] = replacement.symbol
        }

        /**
         * Registers replacement of a simple expression with flattened MFVC instance
         */
        fun registerReplacement(original: IrValueDeclaration, replacement: ValueDeclarationMfvcNodeInstance) {
            oldSymbol2MfvcNodeInstance[original.symbol] = replacement
        }

        fun IrBuilderWithScope.makeReplacement(expression: IrGetValue): IrExpression? {
            oldValueSymbol2NewValueSymbol[expression.symbol]?.let { return irGet(it.owner) }
            val instance = oldSymbol2MfvcNodeInstance[expression.symbol] ?: return null
            val res = instance.makeGetterExpression(this, irCurrentClass, ::registerPossibleExtraBoxUsage)
            expression2MfvcNodeInstanceAccessor[res] = MfvcNodeInstanceAccessor.Getter(instance)
            return res
        }

        private fun splitExpressions(expressions: List<IrExpression>): Pair<List<IrExpression>, List<IrExpression>> {
            val repeatable = expressions.takeLastWhile { it.isRepeatableGetter() }
            return expressions.subList(0, expressions.size - repeatable.size) to repeatable
        }

        fun IrBlockBuilder.addReplacement(expression: IrSetValue, safe: Boolean): IrExpression? {
            oldValueSymbol2NewValueSymbol[expression.symbol]?.let {
                return irSet(it.owner, expression.value).also { irSet -> +irSet }
            }
            val instance = oldSymbol2MfvcNodeInstance[expression.symbol] ?: return null
            val values: List<IrExpression> = makeFlattenedExpressionsWithGivenSafety(
                instance.node, safe, castExpressionToNotNullTypeIfNeeded(expression.value, instance.type)
            )
            val setterExpressions = instance.makeSetterExpressions(this, values)
            expression2MfvcNodeInstanceAccessor[setterExpressions] = MfvcNodeInstanceAccessor.Setter(instance, values)
            +setterExpressions
            return setterExpressions
        }

        /**
         * @param safe whether protect from partial (because of a potential exception) initialization or not
         */
        private fun IrBlockBuilder.makeFlattenedExpressionsWithGivenSafety(
            node: MfvcNode, safe: Boolean, expression: IrExpression
        ) = if (safe) {
            val (forVariables, rest) = splitExpressions(flattenExpression(expression))
            val variables = when (node) {
                is LeafMfvcNode -> forVariables.map { expr -> irTemporary(expr) }
                is MfvcNodeWithSubnodes -> forVariables.zip(node.leaves) { expr, leaf ->
                    irTemporary(expr, nameHint = leaf.fullFieldName.asString())
                }
            }
            variables.map { irGet(it) } + rest
        } else {
            flattenExpression(expression)
        }

        private val IrFieldAccessExpression.field: IrField
            get() = this.symbol.owner

        fun IrBlockBuilder.addReplacement(expression: IrGetField): IrExpression? {
            val field = expression.field
            expression.receiver?.get(this, field.name)?.let { +it; return it }
            val node = replacements.getMfvcFieldNode(field) ?: return null
            val typeArguments = makeTypeArgumentsFromField(expression)
            val instance: ReceiverBasedMfvcNodeInstance =
                node.createInstanceFromBox(this, typeArguments, expression.receiver, AccessType.UseFields, ::variablesSaver)
            val getterExpression = instance.makeGetterExpression(this, irCurrentClass, ::registerPossibleExtraBoxUsage)
            expression2MfvcNodeInstanceAccessor[getterExpression] = MfvcNodeInstanceAccessor.Getter(instance)
            +getterExpression
            return getterExpression
        }

        fun IrBlockBuilder.addReplacement(expression: IrSetField, safe: Boolean): IrExpression? {
            val field = expression.field
            val node = replacements.getMfvcFieldNode(field) ?: return null
            val instance = expression.receiver?.get(this, field.name)?.let {
                (expression2MfvcNodeInstanceAccessor[it] as MfvcNodeInstanceAccessor.Getter).instance
            } ?: node.createInstanceFromBox(
                scope = this,
                typeArguments = makeTypeArgumentsFromField(expression),
                receiver = expression.receiver,
                accessType = AccessType.UseFields,
                saveVariable = ::variablesSaver
            )
            val values: List<IrExpression> = makeFlattenedExpressionsWithGivenSafety(
                node, safe, castExpressionToNotNullTypeIfNeeded(expression.value, node.type)
            )
            val setterExpressions = instance.makeSetterExpressions(this, values)
            expression2MfvcNodeInstanceAccessor[setterExpressions] = MfvcNodeInstanceAccessor.Setter(instance, values)
            +setterExpressions
            return setterExpressions
        }

        fun IrBlockBuilder.addReplacement(expression: IrCall): IrExpression? {
            val function = expression.symbol.owner
            val property = function.property?.takeIf { function.isGetter } ?: return null
            val dispatchReceiver = expression.dispatchReceiver
            dispatchReceiver?.get(this, property.name)?.let { +it; return it }
            val node = replacements.getMfvcPropertyNode(property) ?: return null
            val typeArguments = makeTypeArgumentsFromFunction(expression)
            // Optimization: pure function access to leaf can be replaced with field access if the field itself is accessible
            val instance: ReceiverBasedMfvcNodeInstance =
                node.createInstanceFromBox(this, typeArguments, dispatchReceiver, AccessType.ChooseEffective, ::variablesSaver)
            val getterExpression = instance.makeGetterExpression(this, irCurrentClass, ::registerPossibleExtraBoxUsage)
            expression2MfvcNodeInstanceAccessor[getterExpression] = MfvcNodeInstanceAccessor.Getter(instance)
            +getterExpression
            return getterExpression
        }

        private fun makeTypeArgumentsFromField(expression: IrFieldAccessExpression) = buildMap {
            val field = expression.symbol.owner
            putAll(makeTypeArgumentsFromType(field.type as IrSimpleType))
            expression.receiver?.type?.let { putAll(makeTypeArgumentsFromType(it as IrSimpleType)) }
        }

        private fun makeTypeArgumentsFromFunction(expression: IrCall) = buildMap {
            val function = expression.symbol.owner
            putAll(makeTypeArgumentsFromType(function.returnType as IrSimpleType))
            expression.dispatchReceiver?.type?.let { putAll(makeTypeArgumentsFromType(it as IrSimpleType)) }
        }

        private inline fun IrBuilderWithScope.irContainer(oldBlock: IrContainerExpression, builder: IrBlockBuilder.() -> Unit) =
            oldBlock.run {
                if (isTransparentScope) irComposite(startOffset, endOffset, origin, body = builder)
                else irBlock(startOffset, endOffset, origin, body = builder)
            }

        private fun IrBuilderWithScope.handleSavedExpression(
            expression: IrExpression, handler: IrBuilderWithScope.(accessor: MfvcNodeInstanceAccessor) -> IrExpression?
        ): IrExpression? {
            val accessor = expression2MfvcNodeInstanceAccessor[expression]
            return when {
                accessor != null -> handler(accessor) ?: return null
                expression !is IrContainerExpression -> null
                else -> when (val lastExpression = expression.statements.lastOrNull()) {
                    is IrExpression -> irContainer(expression) {
                        val inner = handleSavedExpression(lastExpression, handler) ?: return null
                        for (oldStatement in expression.statements.subListWithoutLast(1)) {
                            +oldStatement
                        }
                        +inner
                    }

                    else -> null
                }
            }
        }

        fun IrExpression.get(scope: IrBuilderWithScope, name: Name): IrExpression? = scope.handleSavedExpression(this) { accessor ->
            val newAccessor = accessor[name] ?: return@handleSavedExpression null
            val expression = when (newAccessor) {
                is MfvcNodeInstanceAccessor.Setter -> newAccessor.instance.makeSetterExpressions(scope, newAccessor.values)
                is MfvcNodeInstanceAccessor.Getter -> newAccessor.instance.makeGetterExpression(
                    scope, irCurrentClass, ::registerPossibleExtraBoxUsage
                )
            }
            expression2MfvcNodeInstanceAccessor[expression] = newAccessor
            expression
        }

        fun handleFlattenedGetterExpressions(
            scope: IrBuilderWithScope,
            expression: IrExpression,
            handler: IrBlockBuilder.(values: List<IrExpression>) -> IrExpression
        ): IrExpression? = scope.handleSavedExpression(expression) {
            irBlock { +handler(it.instance.makeFlattenedGetterExpressions(this, irCurrentClass, ::registerPossibleExtraBoxUsage)) }
                .unwrapBlock()
        }

        fun registerReplacement(expression: IrExpression, instance: MfvcNodeInstance) {
            expression2MfvcNodeInstanceAccessor[expression] = MfvcNodeInstanceAccessor.Getter(instance)
        }
    }

    private val IrField.property
        get() = correspondingPropertySymbol?.owner
    private val IrSimpleFunction.property
        get() = correspondingPropertySymbol?.owner

    private val valueDeclarationsRemapper = ValueDeclarationRemapper()

    override val replacements
        get() = context.multiFieldValueClassReplacements

    override fun IrClass.isSpecificLoweringLogicApplicable(): Boolean = isMultiFieldValueClass

    override val specificMangle: SpecificMangle
        get() = SpecificMangle.MultiField

    private val variablesToAdd = mutableMapOf<IrDeclarationParent, MutableSet<IrVariable>>()

    private fun variablesSaver(variable: IrVariable) {
        variablesToAdd.getOrPut(variable.parent) { mutableSetOf() }.add(variable)
    }

    override fun visitClassNew(declaration: IrClass): IrClass {

        if (declaration.isSpecificLoweringLogicApplicable()) {
            handleSpecificNewClass(declaration)
        } else {
            handleNonSpecificNewClass(declaration)
        }

        declaration.transformDeclarationsFlat { memberDeclaration ->
            (if (memberDeclaration is IrFunction) withinScope(memberDeclaration) {
                transformFunctionFlat(memberDeclaration)
            } else {
                memberDeclaration.accept(this, null)
                null
            }).also { declarations ->
                for (replacingDeclaration in declarations ?: listOf(memberDeclaration)) {
                    postActionAfterTransformingClassDeclaration(replacingDeclaration)
                }
            }
        }

        return declaration
    }

    override fun visitClassNewDeclarationsWhenParallel(declaration: IrDeclaration) =
        postActionAfterTransformingClassDeclaration(declaration)

    override fun postActionAfterTransformingClassDeclaration(replacingDeclaration: IrDeclaration) {
        when (replacingDeclaration) {
            is IrFunction -> replacingDeclaration.body = replacingDeclaration.body?.makeBodyWithAddedVariables(
                context, variablesToAdd[replacingDeclaration] ?: emptySet(), replacingDeclaration.symbol
            )?.apply {
                if (replacingDeclaration in possibleExtraBoxUsageGenerated) removeAllExtraBoxes()
            }

            is IrAnonymousInitializer -> replacingDeclaration.body = replacingDeclaration.body.makeBodyWithAddedVariables(
                context, variablesToAdd[replacingDeclaration.parent] ?: emptySet(), replacingDeclaration.symbol
            ).apply {
                if (replacingDeclaration in possibleExtraBoxUsageGenerated) removeAllExtraBoxes()
            } as IrBlockBody

            is IrField -> replacingDeclaration.initializer = replacingDeclaration.initializer?.makeBodyWithAddedVariables(
                context, variablesToAdd[replacingDeclaration] ?: emptySet(), replacingDeclaration.symbol
            )?.apply {
                if (replacingDeclaration in possibleExtraBoxUsageGenerated) removeAllExtraBoxes()
            } as IrExpressionBody?

            else -> Unit
        }
    }

    private fun handleNonSpecificNewClass(irClass: IrClass) {
        irClass.primaryConstructor?.let {
            replacements.getReplacementForRegularClassConstructor(it)?.let { replacement -> addBindingsFor(it, replacement) }
        }
        val propertiesOrFields = collectPropertiesOrFieldsAfterLowering(irClass, context)
        val oldBackingFields = buildMap {
            for (propertyOrField in propertiesOrFields) {
                val property = (propertyOrField as? IrPropertyOrIrField.Property)?.property ?: continue
                val field = property.backingField ?: continue
                put(property, field)
            }
        }
        val propertiesOrFieldsReplacement = collectRegularClassMfvcPropertiesOrFieldsReplacement(propertiesOrFields)

        val fieldsToRemove = propertiesOrFieldsReplacement.keys.mapNotNull {
            when (it) {
                is IrPropertyOrIrField.Field -> it.field
                is IrPropertyOrIrField.Property -> oldBackingFields[it.property]
            }
        }.toSet()

        if (fieldsToRemove.isNotEmpty() || propertiesOrFieldsReplacement.isNotEmpty()) {
            val newDeclarations = makeNewDeclarationsForRegularClass(fieldsToRemove, propertiesOrFieldsReplacement, irClass)
            irClass.declarations.replaceAll(newDeclarations)
        }
        for (field in fieldsToRemove) {
            field.correspondingPropertySymbol?.owner?.backingField = null
        }
    }

    private fun collectRegularClassMfvcPropertiesOrFieldsReplacement(propertiesOrFields: LinkedHashSet<IrPropertyOrIrField>) =
        LinkedHashMap<IrPropertyOrIrField, IntermediateMfvcNode>().apply {
            for (propertyOrField in propertiesOrFields) {
                val node = when (propertyOrField) {
                    is IrPropertyOrIrField.Field -> replacements.getMfvcFieldNode(propertyOrField.field)
                    is IrPropertyOrIrField.Property -> replacements.getMfvcPropertyNode(propertyOrField.property)
                } ?: continue
                put(propertyOrField, node as IntermediateMfvcNode)
            }
        }

    private fun makeNewDeclarationsForRegularClass(
        fieldsToRemove: Set<IrField>,
        propertiesOrFieldsReplacement: Map<IrPropertyOrIrField, IntermediateMfvcNode>,
        irClass: IrClass,
    ) = buildList {
        for (element in irClass.declarations) {
            when (element) {
                !is IrField, !in fieldsToRemove -> add(element)
                else -> {
                    val fields = element.property?.let { propertiesOrFieldsReplacement[IrPropertyOrIrField.Property(it)] }?.fields
                        ?: propertiesOrFieldsReplacement[IrPropertyOrIrField.Field(element)]?.fields
                    if (fields != null) {
                        addAll(fields)
                        element.initializer?.let { initializer -> add(makeInitializerReplacement(irClass, element, initializer)) }
                    } else {
                        add(element)
                    }
                }
            }
        }

        for ((propertyOrField, node) in propertiesOrFieldsReplacement.entries) {
            if (propertyOrField is IrPropertyOrIrField.Property) { // they are not used, only boxes are used for them
                addAll(node.allInnerUnboxMethods.filter { it.parent == irClass })
            }
        }
    }

    private fun makeInitializerReplacement(irClass: IrClass, element: IrField, initializer: IrExpressionBody): IrAnonymousInitializer =
        context.irFactory.createAnonymousInitializer(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET, origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER,
            symbol = IrAnonymousInitializerSymbolImpl(),
            isStatic = element.isStatic,
        ).apply {
            parent = irClass
            body = context.createJvmIrBuilder(symbol).irBlockBody {
                +irSetField(
                    receiver = irClass.thisReceiver!!.takeUnless { element.isStatic }?.let { irGet(it) },
                    field = element,
                    value = initializer.expression.patchDeclarationParents(irClass),
                    origin = UNSAFE_MFVC_SET_ORIGIN
                )
            }
            element.initializer = null
        }

    override fun handleSpecificNewClass(declaration: IrClass) {
        val rootNode = replacements.getRootMfvcNode(declaration)
        rootNode.replaceMfvcNotStaticFields()
        declaration.declarations += rootNode.allUnboxMethods + listOfNotNull(
            // `takeIf` is a workaround for double addition problem: user-defined typed equals is already defined in the class
            rootNode.boxMethod, rootNode.specializedEqualsMethod.takeIf { rootNode.createdNewSpecializedEqualsMethod }
        )
        replacePrimaryMultiFieldValueClassConstructor(rootNode)

        replaceMfvcStaticFields(declaration)
    }

    private fun replaceMfvcStaticFields(declaration: IrClass) {
        val staticFieldMapping: Map<IrField, List<IrDeclaration>> = buildMap {
            for (staticField in declaration.fields.filter { it.isStatic }) {
                val node = replacements.getMfvcFieldNode(staticField) ?: continue
                val fields = node.fields ?: listOf()
                val initializer = staticField.initializer?.let { makeInitializerReplacement(declaration, staticField, it) }
                staticField.correspondingPropertySymbol?.owner?.backingField = null
                put(staticField, fields + listOfNotNull(initializer))
            }
        }
        if (staticFieldMapping.isNotEmpty()) {
            declaration.declarations.replaceAll(declaration.declarations.flatMap { staticFieldMapping[it] ?: listOf(it) })
        }
    }

    override fun transformSecondaryConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration> {
        for (param in replacement.valueParameters) {
            param.defaultValue?.patchDeclarationParents(replacement)
            visitParameter(param)
        }

        allScopes.push(createScope(replacement))
        replacement.body = context.createJvmIrBuilder(replacement.symbol).irBlockBody {
            val thisVar = irTemporary(irType = replacement.returnType, nameHint = "\$this")
            constructor.body?.statements?.forEach { statement ->
                +statement.transformStatement(object : IrElementTransformerVoid() {
                    override fun visitClass(declaration: IrClass): IrStatement = declaration

                    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                        val delegatedConstructor = expression.symbol.owner
                        if (delegatedConstructor.constructedClass != constructor.constructedClass) { // Delegating constructor to Object
                            require(delegatedConstructor.constructedClass == context.irBuiltIns.anyClass.owner) {
                                "Expected delegating constructor to the MFVC primary constructor or Any constructor but got: ${delegatedConstructor.render()}"
                            }
                            return irBlock { }
                        }
                        thisVar.initializer = irCall(delegatedConstructor).apply {
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
        allScopes.pop()
        return listOf(replacement)
    }

    private companion object {
        private val UNSAFE_MFVC_SET_ORIGIN by IrStatementOriginImpl
    }

    private fun RootMfvcNode.replaceMfvcNotStaticFields() {
        val fieldsToRemove = mfvc.fields.filter { !it.isStatic }.toList()
        for (field in fieldsToRemove) {
            field.correspondingPropertySymbol?.owner?.backingField = null
        }
        mfvc.declarations.removeAll(fieldsToRemove)
        mfvc.declarations += fields ?: emptyList()
    }

    override fun createBridgeDeclaration(source: IrSimpleFunction, replacement: IrSimpleFunction, mangledName: Name): IrSimpleFunction =
        context.irFactory.buildFun {
            updateFrom(source)
            name = mangledName
            returnType = source.returnType
        }.apply {
            val implementationStructure = if (isFakeOverride) {
                val superDeclaration = replacement.allOverridden().singleOrNull { it.body != null }
                    ?: error("${this.render()} is fake override and has no implementation")
                replacements.bindingNewFunctionToParameterTemplateStructure[superDeclaration]
                    ?: superDeclaration.explicitParameters.map { RegularMapping(it) }
            } else {
                replacements.bindingNewFunctionToParameterTemplateStructure[replacement]
                    ?: error("${replacement.render()} must have MFVC structure")
            }
            copyTypeParametersFrom(source) // without static type parameters
            val substitutionMap = makeTypeParameterSubstitutionMap(source, this)
            dispatchReceiverParameter = source.dispatchReceiverParameter!!.let { // source!!!
                it.copyTo(this, type = it.type.substitute(substitutionMap))
            }
            if ((source.parent as? IrClass)?.isMultiFieldValueClass == true) {
                require(replacement.dispatchReceiverParameter == null) {
                    """
                        Ambiguous receivers:
                        ${source.dispatchReceiverParameter!!.render()}
                        ${replacement.dispatchReceiverParameter!!.render()}
                        """.trimIndent()
                }
            }
            require(replacement.extensionReceiverParameter == null) {
                "Static replacement must have no extension receiver but ${replacement.extensionReceiverParameter!!.render()} found"
            }
            val structure = buildList(implementationStructure.size) {
                var valueParameterIndex = 0
                add(RegularMapping(dispatchReceiverParameter!!))
                val valueParametersAsMutableList = mutableListOf<IrValueParameter>()
                for (expectedParameterStructure in implementationStructure.asSequence().drop(this.size)) {
                    fun IrValueParameter.copy() =
                        copyTo(this@apply, type = type.substitute(substitutionMap), index = valueParameterIndex++)

                    val parameterStructure = when (expectedParameterStructure) {
                        is RegularMapping -> RegularMapping(expectedParameterStructure.valueParameter.copy())
                        is MultiFieldValueClassMapping -> MultiFieldValueClassMapping(
                            expectedParameterStructure.rootMfvcNode,
                            substitutionMap,
                            expectedParameterStructure.valueParameters.map { it.copy() }
                        )
                    }
                    add(parameterStructure)
                    valueParametersAsMutableList.addAll(parameterStructure.valueParameters)
                }
                valueParameters = valueParametersAsMutableList
            }
            replacements.bindingNewFunctionToParameterTemplateStructure[this] = structure
            annotations = source.annotations
            parent = source.parent
            // We need to ensure that this bridge has the same attribute owner as its static inline class replacement, since this
            // is used in [CoroutineCodegen.isStaticInlineClassReplacementDelegatingCall] to identify the bridge and avoid generating
            // a continuation class.
            copyAttributes(source)
        }

    override fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction, original: IrFunction, inverted: Boolean) {
        allScopes.push(createScope(source))
        source.body = with(context.createJvmIrBuilder(source.symbol)) {
            irExprBody(irBlock {
                val parameters2arguments = replacements.mapFunctionMfvcStructures(this, target, source) { sourceParameter, _ ->
                    irGet(sourceParameter)
                }
                +irReturn(irCall(target).apply {
                    passTypeArgumentsWithOffsets(target, source) { source.typeParameters[it].defaultType }
                    for ((parameter, argument) in parameters2arguments) {
                        if (argument != null) {
                            putArgument(parameter, argument)
                        }
                    }
                })
            })
        }
        allScopes.pop()
    }

    private fun IrMemberAccessExpression<*>.passTypeArgumentsWithOffsets(
        target: IrFunction, source: IrFunction, forCommonTypeParameters: (targetIndex: Int) -> IrType
    ) {
        val passedTypeParametersSize = minOf(target.typeParameters.size, source.typeParameters.size)
        val targetOffset = target.typeParameters.size - passedTypeParametersSize
        val sourceOffset = source.typeParameters.size - passedTypeParametersSize
        if (sourceOffset > 0) {
            // static fun calls method
            val dispatchReceiverType = source.parentAsClass.defaultType
            require(dispatchReceiverType.let {
                it.needsMfvcFlattening() && it.erasedUpperBound.typeParameters.size == sourceOffset
            }) { "Unexpected dispatcher receiver type: ${dispatchReceiverType.render()}" }
        }
        if (targetOffset > 0) {
            // method calls static fun
            val dispatchReceiverType = source.parentAsClass.defaultType
            require(dispatchReceiverType.let {
                it.needsMfvcFlattening() && it.erasedUpperBound.typeParameters.size == targetOffset
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
            val (oldParameter, newParamList) = old2newList[i]
            when (val structure = parametersStructure[i]) {
                is RegularMapping -> valueDeclarationsRemapper.registerReplacement(oldParameter, newParamList.single())
                is MultiFieldValueClassMapping -> {
                    val mfvcNodeInstance = structure.rootMfvcNode.createInstanceFromValueDeclarationsAndBoxType(
                        structure.boxedType, newParamList
                    )
                    valueDeclarationsRemapper.registerReplacement(oldParameter, mfvcNodeInstance)
                }
            }
        }

        withinScope(replacement) {
            addDefaultArgumentsIfNeeded(replacement, old2newList, parametersStructure)
        }
    }

    private fun addDefaultArgumentsIfNeeded(
        replacement: IrFunction,
        old2newList: List<Pair<IrValueParameter, List<IrValueParameter>>>,
        parametersStructure: List<RemappedParameter>
    ) {
        for (i in old2newList.indices) {
            val (param, newParamList) = old2newList[i]
            val defaultValue = replacements.oldMfvcDefaultArguments[param] ?: continue
            val structure = parametersStructure[i]
            if (structure is MultiFieldValueClassMapping) {
                val fakeFunction = context.irFactory.buildFun {
                    returnType = context.irBuiltIns.unitType
                    name = Name.identifier("fake")
                    visibility = DescriptorVisibilities.LOCAL
                }.apply { parent = replacement }
                newParamList[0].defaultValue = with(context.createJvmIrBuilder(fakeFunction.symbol)) {
                    withinScope(fakeFunction) {
                        fakeFunction.body = irExprBody(irBlock {
                            val mfvcNodeInstance = structure.rootMfvcNode.createInstanceFromValueDeclarationsAndBoxType(
                                param.type as IrSimpleType, newParamList
                            )
                            flattenExpressionTo(defaultValue, mfvcNodeInstance)
                            +irGet(newParamList[0])
                        })
                        postActionAfterTransformingClassDeclaration(fakeFunction)
                        fakeFunction.body?.patchDeclarationParents(replacement) as IrExpressionBody
                    }
                }
            }
        }
    }

    override fun visitParameter(parameter: IrValueParameter) {
        // default MFVC parameter is the special case, it is handled separately
        if (parameter.origin == JvmLoweredDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER) return
        if (parameter.defaultValue == null) return
        val fakeFunction = context.irFactory.buildFun {
            returnType = context.irBuiltIns.unitType
            name = Name.identifier("fake")
            visibility = DescriptorVisibilities.LOCAL
        }.apply { parent = parameter.parent }
        parameter.defaultValue = with(context.createJvmIrBuilder(fakeFunction.symbol)) {
            withinScope(fakeFunction) {
                fakeFunction.body = irExprBody(parameter.defaultValue!!.expression).transform(this@JvmMultiFieldValueClassLowering, null)
                postActionAfterTransformingClassDeclaration(fakeFunction)
                fakeFunction.body?.patchDeclarationParents(parameter.parent) as IrExpressionBody
            }
        }
    }

    private fun replacePrimaryMultiFieldValueClassConstructor(rootMfvcNode: RootMfvcNode) {
        rootMfvcNode.mfvc.declarations.removeIf { it is IrConstructor && it.isPrimary }
        val newPrimaryConstructor = rootMfvcNode.newPrimaryConstructor
        rootMfvcNode.throwWhenNotExternalIsNull(newPrimaryConstructor)
        val primaryConstructorImpl = rootMfvcNode.primaryConstructorImpl
        rootMfvcNode.throwWhenNotExternalIsNull(primaryConstructorImpl)
        rootMfvcNode.mfvc.declarations += listOf(newPrimaryConstructor, primaryConstructorImpl)

        val initializersBlocks = rootMfvcNode.mfvc.declarations.filterIsInstance<IrAnonymousInitializer>()
        val typeArguments = makeTypeParameterSubstitutionMap(rootMfvcNode.mfvc, primaryConstructorImpl)
        if (!rootMfvcNode.mfvc.isKotlinExternalStub()) {
            val oldPrimaryConstructor = rootMfvcNode.oldPrimaryConstructor
            rootMfvcNode.throwWhenNotExternalIsNull(oldPrimaryConstructor)
            primaryConstructorImpl.body = context.createJvmIrBuilder(primaryConstructorImpl.symbol).irBlockBody {
                val mfvcNodeInstance = ValueDeclarationMfvcNodeInstance(rootMfvcNode, typeArguments, primaryConstructorImpl.valueParameters)
                valueDeclarationsRemapper.registerReplacement(oldPrimaryConstructor.constructedClass.thisReceiver!!, mfvcNodeInstance)
                for (initializer in initializersBlocks) {
                    +irBlock {
                        for (stmt in initializer.body.statements) {
                            +stmt.patchDeclarationParents(rootMfvcNode.primaryConstructorImpl) // transformation is done later
                        }
                    }
                }
            }
        }
        rootMfvcNode.mfvc.declarations.removeIf { it is IrAnonymousInitializer }
    }

    private fun IrBlock.hasLambdaLikeOrigin() = origin == IrStatementOrigin.LAMBDA || origin == IrStatementOrigin.ANONYMOUS_FUNCTION

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        if (expression is IrBlock && expression.hasLambdaLikeOrigin() && expression.statements.any { it is IrFunctionReference }) {
            return visitLambda(expression)
        }
        return super.visitContainerExpression(expression)
    }

    private fun visitLambda(irBlock: IrBlock): IrExpression {
        require(irBlock.hasLambdaLikeOrigin() && irBlock.statements.size == 2) { "Illegal lambda: ${irBlock.dump()}" }
        val (originalFunction, ref) = irBlock.statements
        require(originalFunction is IrSimpleFunction && ref is IrFunctionReference && ref.symbol.owner == originalFunction) { "Illegal lambda: ${irBlock.dump()}" }
        require(originalFunction == irBlock.statements.first()) { "Illegal lambda: ${irBlock.dump()}" }
        val replacement = originalFunction.getReplacement()
        if (replacement == null) {
            irBlock.statements[0] = visitFunctionNew(originalFunction)
            return irBlock
        }
        transformFunctionFlat(originalFunction).let { declarations ->
            require(declarations == listOf(replacement)) {
                "Expected ${replacement.render()}, got ${declarations?.map { it.render() }}"
            }
        }
        return makeNewLambda(originalFunction, ref, makeBody = { wrapper ->
            variablesToAdd[replacement]?.let {
                variablesToAdd[wrapper] = it
                variablesToAdd.remove(replacement)
            }
            if (replacement in possibleExtraBoxUsageGenerated) {
                possibleExtraBoxUsageGenerated.add(wrapper)
                possibleExtraBoxUsageGenerated.remove(replacement)
            }
            with(context.createJvmIrBuilder(wrapper.symbol)) {
                irExprBody(irBlock {
                    val newArguments: List<IrValueDeclaration> = wrapper.explicitParameters.flatMap { parameter ->
                        if (!parameter.type.needsMfvcFlattening()) {
                            listOf(parameter)
                        } else {
                            // Old parameter value will be only used to set parameter of the lowered function,
                            // thus it is useless to show it in debugger
                            parameter.origin = JvmLoweredDeclarationOrigin.TEMPORARY_MULTI_FIELD_VALUE_CLASS_PARAMETER
                            val rootNode = replacements.getRootMfvcNode(parameter.type.erasedUpperBound)
                            rootNode.createInstanceFromBox(this, irGet(parameter), AccessType.ChooseEffective, ::variablesSaver)
                                .makeFlattenedGetterExpressions(this, irCurrentClass, ::registerPossibleExtraBoxUsage)
                                .mapIndexed { index, expression ->
                                    savableStandaloneVariableWithSetter(
                                        expression = expression,
                                        name = "${parameter.name.asString()}-${rootNode.leaves[index].fullFieldName}",
                                        origin = JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE,
                                        saveVariable = ::variablesSaver,
                                    )
                                }
                        }
                    }
                    +replacement.inline(wrapper.parent, newArguments)
                })
            }
        })
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val function = expression.symbol.owner
        val replacement = function.getReplacement() ?: return super.visitFunctionReference(expression)
        return context.createJvmIrBuilder(expression.symbol, expression).irBlock {
            // Bridge call is added in BridgeLowering
            buildReplacement(function, expression, replacement) {
                IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type, replacement.symbol, function.typeParameters.size, replacement.valueParameters.size,
                    expression.reflectionTarget, expression.origin
                ).copyAttributes(expression)
            }
        }.unwrapBlock()
    }

    private fun IrFunction.getReplacement(): IrFunction? = replacements.getReplacementFunction(this)
        ?: (this as? IrConstructor)?.let { replacements.getReplacementForRegularClassConstructor(it) }

    private fun makeNewLambda(
        originalFunction: IrFunction, expression: IrFunctionReference, makeBody: (wrapper: IrSimpleFunction) -> IrBody
    ): IrContainerExpression {
        val currentDeclarationParent = currentDeclarationParent!!
        val wrapper = context.irFactory.buildFun {
            updateFrom(originalFunction)
            modality = Modality.FINAL
            isFakeOverride = false
            returnType = originalFunction.returnType
            name = originalFunction.name
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = currentDeclarationParent
            assert(typeParameters.isEmpty())
            copyTypeParametersFrom(originalFunction)
            val substitutionMap = makeTypeParameterSubstitutionMap(originalFunction, this)
            require(originalFunction.dispatchReceiverParameter == null || originalFunction.extensionReceiverParameter == null) {
                "${originalFunction.render()} has both a member and an extension receivers at the same time.\nReferences to such elements are not allowed"
            }
            extensionReceiverParameter = (originalFunction.dispatchReceiverParameter ?: originalFunction.extensionReceiverParameter)
                ?.let { it.copyTo(this, type = it.type.substitute(substitutionMap)) }
            valueParameters = originalFunction.valueParameters.mapIndexed { index, param ->
                param.copyTo(this, index = index, type = param.type.substitute(substitutionMap))
            }
            withinScope(this) {
                body = makeBody(this)
                postActionAfterTransformingClassDeclaration(this)
            }
        }

        val newReference = IrFunctionReferenceImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = expression.type,
            symbol = wrapper.symbol,
            typeArgumentsCount = expression.typeArgumentsCount,
            valueArgumentsCount = expression.valueArgumentsCount,
            reflectionTarget = expression.reflectionTarget,
            origin = expression.origin,
        ).apply {
            copyTypeArgumentsFrom(expression)
            extensionReceiver = (expression.dispatchReceiver ?: expression.extensionReceiver)
                ?.transform(this@JvmMultiFieldValueClassLowering, null)
            for ((index, arg) in wrapper.valueParameters.indices zip List(expression.valueArgumentsCount, expression::getValueArgument)) {
                putValueArgument(index, arg?.transform(this@JvmMultiFieldValueClassLowering, null))
            }
            copyAttributes(expression)
            context.getLocalClassType(expression.attributeOwnerId)?.let { context.putLocalClassType(this, it) }
        }
        return context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).irBlock(origin = IrStatementOrigin.LAMBDA) {
            +wrapper
            +newReference
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner
        val currentScope = currentScope!!.irElement as IrDeclaration
        val replacement = replacements.getReplacementFunction(function)
        return when {
            function is IrConstructor && function.isPrimary && function.constructedClass.isMultiFieldValueClass &&
                    currentScope.origin != JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER -> {
                context.createJvmIrBuilder(currentScope.symbol, expression).irBlock {
                    val rootNode = replacements.getRootMfvcNode(function.constructedClass)
                    val instance = rootNode.createInstanceFromValueDeclarationsAndBoxType(
                        scope = this,
                        type = function.constructedClassType as IrSimpleType,
                        name = Name.identifier("constructor_tmp"),
                        saveVariable = ::variablesSaver,
                        isVar = false,
                        origin = JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE
                    )
                    for (valueDeclaration in instance.valueDeclarations) {
                        valueDeclaration.origin = JvmLoweredDeclarationOrigin.TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE
                    }
                    flattenExpressionTo(expression, instance)
                    val getterExpression = instance.makeGetterExpression(this, irCurrentClass, ::registerPossibleExtraBoxUsage)
                    valueDeclarationsRemapper.registerReplacement(getterExpression, instance)
                    +getterExpression
                }
            }

            replacement != null -> context.createJvmIrBuilder(currentScope.symbol, expression).irBlock {
                buildReplacement(function, expression, replacement)
            }.unwrapBlock()

            else -> {
                val newConstructor = (function as? IrConstructor)
                    ?.let { replacements.getReplacementForRegularClassConstructor(it) }
                    ?: return super.visitFunctionAccess(expression)
                val callFactory: (IrConstructorSymbol) -> IrFunctionAccessExpression = when (expression) {
                    is IrConstructorCall -> { constructorSymbol ->
                        IrConstructorCallImpl.fromSymbolOwner(
                            expression.startOffset, expression.endOffset, expression.type, constructorSymbol, expression.origin
                        )
                    }
                    is IrDelegatingConstructorCall -> { constructorSymbol ->
                        IrDelegatingConstructorCallImpl.fromSymbolOwner(
                            expression.startOffset, expression.endOffset, expression.type, constructorSymbol
                        )
                    }
                    is IrEnumConstructorCall -> { constructorSymbol ->
                        IrEnumConstructorCallImpl(
                            expression.startOffset, expression.endOffset, expression.type, constructorSymbol,
                            expression.typeArgumentsCount, expression.valueArgumentsCount
                        )
                    }
                    else -> error("Unknown constructor call type:\n${expression.dump()}")
                }
                context.createJvmIrBuilder(currentScope.symbol, expression).irBlock {
                    buildReplacement(function, expression, newConstructor) { callFactory(it as IrConstructorSymbol) }
                }.unwrapBlock()
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        val property = callee.property
        if (property != null && callee.isGetter && replacements.getMfvcPropertyNode(property) != null) {
            require(callee.valueParameters.isEmpty()) { "Unexpected getter:\n${callee.dump()}" }
            expression.dispatchReceiver = expression.dispatchReceiver?.transform(this, null)
            return context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).irBlock {
                with(valueDeclarationsRemapper) {
                    addReplacement(expression) ?: return expression
                }
            }.unwrapBlock()
        }
        if (expression.isSpecializedMFVCEqEq) {
            return context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).irBlock {
                val leftArgument = expression.getValueArgument(0)!!
                val rightArgument = expression.getValueArgument(1)!!
                val leftClass = leftArgument.type.erasedUpperBound
                val leftNode = if (leftArgument.type.needsMfvcFlattening()) replacements.getRootMfvcNodeOrNull(leftClass) else null
                val rightClass = rightArgument.type.erasedUpperBound
                val rightNode = if (rightArgument.type.needsMfvcFlattening()) replacements.getRootMfvcNodeOrNull(rightClass) else null
                if (leftNode != null) {
                    val newEquals = if (rightNode != null) {
                        require(leftNode == rightNode) { "Different nodes: $leftNode, $rightNode" }
                        // both are unboxed
                        leftNode.specializedEqualsMethod
                    } else {
                        // left one is unboxed, right is not
                        leftClass.functions.single { it.isEquals() }
                    }
                    +irCall(newEquals).apply {
                        dispatchReceiver = leftArgument
                        putValueArgument(0, rightArgument)
                    }.transform(this@JvmMultiFieldValueClassLowering, null)
                } else if (rightNode != null) {
                    // left one is boxed, right one is unboxed
                    if (leftArgument.isNullConst()) {
                        // left argument is always null, right one is unboxed
                        val hasPureFlattenedGetters = rightNode.mapLeaves { it.hasPureUnboxMethod }.all { it }
                        if (hasPureFlattenedGetters) {
                            val rightExpressions = flattenExpression(rightArgument)
                            require((rightExpressions.size > 1) == rightArgument.type.needsMfvcFlattening()) {
                                "Illegal flattening of ${rightArgument.dump()}\n\n${rightExpressions.joinToString("\n") { it.dump() }}"
                            }
                            rightExpressions.filterNot { it.isRepeatableGetter() }.forEach { +it }
                        } else {
                            +rightArgument.transform(this@JvmMultiFieldValueClassLowering, null)
                        }
                        +irFalse()
                    } else if (leftArgument.type.erasedUpperBound == rightArgument.type.erasedUpperBound && leftArgument.type.isNullable()) {
                        // left argument can be unboxed if it is not null, right one is unboxed
                        +irBlock {
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
                    } else {
                        // right one is unboxed but left one is boxed and no intrinsics can be used
                        return super.visitCall(expression)
                    }
                } else {
                    // both are boxed
                    return super.visitCall(expression)
                }
            }.unwrapBlock()
        }
        return super.visitCall(expression)
    }

    private fun IrBlockBuilder.buildReplacement(
        originalFunction: IrFunction,
        original: IrMemberAccessExpression<*>,
        replacement: IrFunction,
        makeMemberAccessExpression: (IrFunctionSymbol) -> IrMemberAccessExpression<*> = { callee: IrFunctionSymbol ->
            irCall(callee.owner, superQualifierSymbol = (original as? IrCall)?.superQualifierSymbol)
        },
    ): IrMemberAccessExpression<*> {
        val parameter2expression = typedArgumentList(originalFunction, original)
        val structure = replacements.bindingOldFunctionToParameterTemplateStructure[originalFunction]!!
        require(parameter2expression.size == structure.size)
        require(structure.sumOf { it.valueParameters.size } == replacement.explicitParametersCount)
        val newArguments: List<IrExpression?> =
            makeNewArguments(parameter2expression.map { (_, argument) -> argument }, structure)
        val resultExpression = makeMemberAccessExpression(replacement.symbol).apply {
            passTypeArgumentsWithOffsets(replacement, originalFunction) { original.getTypeArgument(it)!! }
            for ((parameter, argument) in replacement.explicitParameters zip newArguments) {
                if (argument == null) continue
                putArgument(replacement, parameter, argument)
            }
        }
        +resultExpression
        return resultExpression
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        for (i in expression.arguments.indices) {
            val argument = expression.arguments[i]
            if (argument.type.needsMfvcFlattening()) {
                expression.arguments[i] = context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).run {
                    val toString = argument.type.erasedUpperBound.functions.single { it.isToString() }
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
        oldArguments: List<IrExpression?>, structure: List<RemappedParameter>
    ): List<IrExpression?> {
        val argumentSizes: List<Int> = structure.map { argTemplate -> argTemplate.valueParameters.size }
        val newArguments = (oldArguments zip argumentSizes).flatMapIndexed { index, (oldArgument, parametersCount) ->
            when {
                oldArgument == null -> List(parametersCount) { null }
                parametersCount == 1 -> listOf(oldArgument.transform(this@JvmMultiFieldValueClassLowering, null))
                else -> {
                    val expectedType = (structure[index] as MultiFieldValueClassMapping).boxedType
                    val castedIfNeeded = castExpressionToNotNullTypeIfNeeded(oldArgument, expectedType)
                    flattenExpression(castedIfNeeded).also {
                        require(it.size == parametersCount) { "Expected $parametersCount arguments but got ${it.size}" }
                    }
                }
            }
        }
        return newArguments
    }

    private fun IrBuilderWithScope.castExpressionToNotNullTypeIfNeeded(expression: IrExpression, type: IrType) = when (type) {
        expression.type -> expression
        expression.type.makeNotNull() -> irImplicitCast(expression, type)
        else -> irAs(expression, type)
    }

    /**
     * Inlines initialization of variables when possible and returns their values
     *
     * Example:
     * Before:
     * val a = 2
     * val b = 3
     * val c = b + 1
     * [a, b, c]
     *
     * After:
     * val a = 2
     * val b = 3
     * [a, b, b + 1]
     */
    private fun IrBuilderWithScope.removeExtraSetVariablesFromExpressionList(
        block: IrContainerExpression,
        variables: List<IrVariable>
    ): List<IrExpression> {
        val forbiddenVariables = mutableSetOf<IrVariable>()
        val variablesSet = variables.toSet()
        val standaloneExpressions = mutableListOf<IrExpression>()
        val resultVariables = variables.toMutableList()

        fun recur(block: IrContainerExpression): Boolean /* stop optimization */ {
            while (block.statements.isNotEmpty() && resultVariables.isNotEmpty()) {
                val statement = block.statements.last()
                //also stop
                when {
                    statement is IrContainerExpression -> if (recur(statement)) {
                        if (statement.statements.isEmpty()) {
                            block.statements.removeLast()
                        }
                        return true
                    } else {
                        require(statement.statements.isEmpty() || resultVariables.isEmpty()) { "Not all statements removed" }
                        if (statement.statements.isEmpty()) {
                            block.statements.removeLast()
                        }
                    }

                    statement !is IrSetValue -> return true
                    statement.symbol.owner != resultVariables.last() -> return true
                    statement.symbol.owner in forbiddenVariables -> return true
                    else -> {
                        standaloneExpressions.add(statement.value)
                        resultVariables.removeLast()
                        block.statements.removeLast()
                        statement.value.acceptVoid(object : IrElementVisitorVoid {
                            override fun visitElement(element: IrElement) {
                                element.acceptChildrenVoid(this)
                            }

                            override fun visitValueAccess(expression: IrValueAccessExpression) {
                                val valueDeclaration = expression.symbol.owner
                                if (valueDeclaration is IrVariable && valueDeclaration in variablesSet) {
                                    forbiddenVariables.add(valueDeclaration)
                                }
                                super.visitValueAccess(expression)
                            }
                        })
                    }
                }
            }
            return false
        }
        recur(block)
        return resultVariables.map { irGet(it) } + standaloneExpressions.asReversed()
    }

    // Note that reference equality (x === y) is not allowed on values of MFVC class type,
    // so it is enough to check for eqeq.
    private val IrCall.isSpecializedMFVCEqEq: Boolean
        get() = symbol == context.irBuiltIns.eqeqSymbol &&
                listOf(getValueArgument(0), getValueArgument(1))
                    .any { it!!.type.erasedUpperBound.isMultiFieldValueClass }

    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.receiver = expression.receiver?.transform(this, null)
        with(valueDeclarationsRemapper) {
            return context.createJvmIrBuilder(expression.symbol, expression).irBlock {
                addReplacement(expression) ?: return expression
            }.unwrapBlock()
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        expression.receiver = expression.receiver?.transform(this, null)
        with(valueDeclarationsRemapper) {
            return context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).irBlock {
                addReplacement(expression, safe = expression.origin != UNSAFE_MFVC_SET_ORIGIN)
                    ?: return expression.also { it.value = it.value.transform(this@JvmMultiFieldValueClassLowering, null) }
            }.unwrapBlock()
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression =
        with(valueDeclarationsRemapper) {
            context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).makeReplacement(expression) ?: super.visitGetValue(expression)
        }

    override fun visitSetValue(expression: IrSetValue): IrExpression =
        context.createJvmIrBuilder(getCurrentScopeSymbol(), expression).irBlock {
            with(valueDeclarationsRemapper) {
                addReplacement(expression, safe = expression.origin != UNSAFE_MFVC_SET_ORIGIN)
                    ?: return super.visitSetValue(expression)
            }
        }.unwrapBlock()

    private val IrDeclarationOrigin.isTemporary
        get() =
            this == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                    || this == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER
                    || this == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val initializer = declaration.initializer
        if (declaration.type.needsMfvcFlattening()) {
            val irClass = declaration.type.erasedUpperBound
            val rootNode = replacements.getRootMfvcNode(irClass)
            return context.createJvmIrBuilder(getCurrentScopeSymbol(), declaration).irBlock {
                val origin = if (declaration.origin.isTemporary) {
                    JvmLoweredDeclarationOrigin.TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE
                } else {
                    JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE
                }
                val instance = rootNode.createInstanceFromValueDeclarationsAndBoxType(
                    this, declaration.type as IrSimpleType, declaration.name, ::variablesSaver, declaration.isVar, origin
                )
                valueDeclarationsRemapper.registerReplacement(declaration, instance)
                initializer?.let {
                    flattenExpressionTo(it, instance)
                }
            }.unwrapBlock()
        }
        return super.visitVariable(declaration)
    }

    private fun getCurrentScopeSymbol() = (currentScope!!.irElement as IrSymbolOwner).symbol

    /**
     * Takes not transformed expression and returns its flattened transformed representation (expressions)
     */
    fun IrBlockBuilder.flattenExpression(expression: IrExpression): List<IrExpression> {
        if (!expression.type.needsMfvcFlattening()) {
            return listOf(expression.transform(this@JvmMultiFieldValueClassLowering, null))
        }
        val rootMfvcNode = replacements.getRootMfvcNode(expression.type.erasedUpperBound)
        val typeArguments = makeTypeArgumentsFromType(expression.type as IrSimpleType)
        val variables = rootMfvcNode.leaves.map {
            savableStandaloneVariable(
                type = it.type.substitute(typeArguments),
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                saveVariable = ::variablesSaver,
                isVar = false,
            )
        }
        val instance = ValueDeclarationMfvcNodeInstance(rootMfvcNode, typeArguments, variables)
        val block = irBlock {
            flattenExpressionTo(expression, instance)
        }
        val expressions = removeExtraSetVariablesFromExpressionList(block, variables)
        if (block.statements.isNotEmpty()) {
            +block.unwrapBlock()
        }
        return expressions
    }

    /**
     * Takes not transformed expression and initialized given MfvcNodeInstance with transformed version of it
     */
    private fun IrBlockBuilder.flattenExpressionTo(expression: IrExpression, instance: MfvcNodeInstance) {
        val rootNode = replacements.getRootMfvcNodeOrNull(
            if (expression is IrConstructorCall) expression.symbol.owner.constructedClass else expression.type.erasedUpperBound
        )
        val type = if (expression is IrConstructorCall) expression.symbol.owner.constructedClass.defaultType else expression.type
        if (type == context.irBuiltIns.nothingType) {
            return flattenExpressionTo(irImplicitCast(expression, instance.type), instance)
        }
        val lowering = this@JvmMultiFieldValueClassLowering
        if (rootNode == null || !type.needsMfvcFlattening() || instance.size == 1) {
            require(instance.size == 1) { "Required 1 variable/field to store regular value but got ${instance.size}" }
            instance.addSetterStatements(this, listOf(expression.transform(lowering, null)))
            return
        }
        require(rootNode.leavesCount == instance.size) {
            "Required ${rootNode.leavesCount} variable/field to store regular value but got ${instance.size}"
        }
        if (expression is IrWhen) {
            for (branch in expression.branches) {
                branch.condition = branch.condition.transform(lowering, null)
                branch.result = irBlock {
                    flattenExpressionTo(branch.result, instance)
                }.unwrapBlock()
            }
            +expression
            return
        }
        if (expression is IrTry) {
            expression.tryResult = irBlock { flattenExpressionTo(expression.tryResult, instance) }.unwrapBlock()
            expression.catches.replaceAll { irCatch(it.catchParameter, irBlock { flattenExpressionTo(it.result, instance) }.unwrapBlock()) }
            expression.finallyExpression = expression.finallyExpression?.transform(lowering, null)
            +expression
            return
        }
        if (expression is IrConstructorCall) {
            val constructor = expression.symbol.owner
            if (constructor.isPrimary && constructor.constructedClass.isMultiFieldValueClass &&
                constructor.origin != JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR
            ) {
                val oldArguments = List(expression.valueArgumentsCount) {
                    expression.getValueArgument(it)
                        ?: error("Default arguments for MFVC primary constructors are not yet supported:\n${expression.dump()}")
                }
                require(rootNode.subnodes.size == oldArguments.size) {
                    "Old ${constructor.render()} must have ${rootNode.subnodes.size} arguments but got ${oldArguments.size}"
                }
                for ((subnode, argument) in rootNode.subnodes zip oldArguments) {
                    flattenExpressionTo(argument, instance[subnode.name]!!)
                }
                +irCall(rootNode.primaryConstructorImpl.let { rootNode.throwWhenNotExternalIsNull(it); it }).apply {
                    copyTypeArgumentsFrom(expression)
                    val flattenedGetterExpressions =
                        instance.makeFlattenedGetterExpressions(this@flattenExpressionTo, irCurrentClass, ::registerPossibleExtraBoxUsage)
                    for ((index, leafExpression) in flattenedGetterExpressions.withIndex()) {
                        putValueArgument(index, leafExpression)
                    }
                }
                return
            }
        }
        val nullableTransformedExpression = expression.transform(this@JvmMultiFieldValueClassLowering, null)
        val transformedExpression = castExpressionToNotNullTypeIfNeeded(nullableTransformedExpression, instance.type)
        val addedSettersToFlattened = valueDeclarationsRemapper.handleFlattenedGetterExpressions(this, transformedExpression) {
            require(it.size == instance.size) { "Incompatible assignment sizes: ${it.size}, ${instance.size}" }
            instance.makeSetterExpressions(this, it)
        }
        if (addedSettersToFlattened != null) {
            +addedSettersToFlattened
            return
        }
        val expressionInstance = rootNode.createInstanceFromBox(
            this, transformedExpression, AccessType.ChooseEffective, ::variablesSaver,
        )
        require(expressionInstance.size == instance.size) { "Incompatible assignment sizes: ${expressionInstance.size}, ${instance.size}" }
        instance.addSetterStatements(
            this, expressionInstance.makeFlattenedGetterExpressions(this, irCurrentClass, ::registerPossibleExtraBoxUsage)
        )
    }

    /**
     * Removes boxing when the result is not used
     */
    private fun IrBody.removeAllExtraBoxes() {
        // data is whether the expression result is used
        accept(object : IrElementVisitor<Unit, Boolean> {
            override fun visitElement(element: IrElement, data: Boolean) {
                element.acceptChildren(this, true) // uses what is inside
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Boolean) {
                expression.acceptChildren(this, data) // type operator calls are transparent
            }

            private tailrec fun getFunctionCallOrNull(statement: IrStatement): IrCall? = when (statement) {
                is IrTypeOperatorCall -> getFunctionCallOrNull(statement.argument)
                is IrCall -> statement
                else -> null
            }

            // inner functions will be handled separately, no need to do it now
            override fun visitFunction(declaration: IrFunction, data: Boolean) = Unit

            // inner classes will be handled separately, no need to do it now
            override fun visitClass(declaration: IrClass, data: Boolean) = Unit

            override fun visitContainerExpression(expression: IrContainerExpression, data: Boolean) {
                handleStatementContainer(expression, data)
            }

            override fun visitWhen(expression: IrWhen, data: Boolean) {
                expression.acceptChildren(this, data) // when's are transparent
            }

            override fun visitCatch(aCatch: IrCatch, data: Boolean) {
                aCatch.acceptChildren(this, data) // catches are transparent
            }

            override fun visitTry(aTry: IrTry, data: Boolean) {
                aTry.tryResult.accept(this, data)
                aTry.catches.forEach { it.accept(this, data) }
                aTry.finallyExpression?.accept(this, false)
            }

            override fun visitBranch(branch: IrBranch, data: Boolean) {
                branch.condition.accept(this, true)
                branch.result.accept(this, data)
            }

            override fun visitBlockBody(body: IrBlockBody, data: Boolean) {
                handleStatementContainer(body, data)
            }

            private fun handleStatementContainer(expression: IrStatementContainer, resultIsUsed: Boolean) {
                for (statement in expression.statements.subListWithoutLast(1)) {
                    statement.accept(this, false)
                }
                expression.statements.lastOrNull()?.accept(this, resultIsUsed)
                val statementsToRemove = mutableSetOf<IrStatement>()
                for (statement in expression.statements.subListWithoutLast(if (resultIsUsed) 1 else 0)) {
                    val call = getFunctionCallOrNull(statement) ?: continue
                    val node = replacements.getRootMfvcNodeOrNull(call.type.erasedUpperBound) ?: continue
                    if (node.boxMethod == call.symbol.owner &&
                        List(call.valueArgumentsCount) { call.getValueArgument(it) }.all { it.isRepeatableGetter() }
                    ) {
                        statementsToRemove.add(statement)
                    }
                }
                expression.statements.removeIf { it in statementsToRemove }
            }
        }, false)
    }
}

// like dropLast but creates a view to the original list instead of copying content of it
private fun <T> List<T>.subListWithoutLast(n: Int) = if (size > n) subList(0, size - n) else emptyList()

private sealed class BlockOrBody {
    abstract val element: IrElement

    data class Body(val body: IrBody) : BlockOrBody() {
        override val element get() = body
    }

    data class Block(val block: IrBlock) : BlockOrBody() {
        override val element get() = block
    }
}

/**
 * Finds the most narrow block or body which contains all usages of each of the given variables
 */
private fun findNearestBlocksForVariables(variables: Set<IrVariable>, body: BlockOrBody): Map<IrVariable, BlockOrBody?> {
    if (variables.isEmpty()) return mapOf()
    val variableUsages = mutableMapOf<BlockOrBody, MutableSet<IrVariable>>()
    val childrenBlocks = mutableMapOf<BlockOrBody, MutableList<BlockOrBody>>()

    body.element.acceptVoid(object : IrElementVisitorVoid {
        private val stack = mutableListOf<BlockOrBody>()
        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        override fun visitBody(body: IrBody) {
            currentStackElement()?.let { childrenBlocks.getOrPut(it) { mutableListOf() }.add(BlockOrBody.Body(body)) }
            stack.add(BlockOrBody.Body(body))
            super.visitBody(body)
            require(stack.removeLast() == BlockOrBody.Body(body)) { "Invalid stack" }
        }

        override fun visitBlock(expression: IrBlock) {
            // This is a workaround.
            // We process IrInlinedFunctionBlock on codegen in a special way by processing composite blocks with arguments evaluation first.
            // Thus, we don't want IrInlinedFunctionBlock to contain variable declarations before this composite blocks.
            // That is why we move variable declarations from IrInlinedFunctionBlock to the outer block.
            if (expression is IrInlinedFunctionBlock) {
                return super.visitBlock(expression)
            }
            currentStackElement()?.let { childrenBlocks.getOrPut(it) { mutableListOf() }.add(Block(expression)) }
            stack.add(Block(expression))
            super.visitBlock(expression)
            require(stack.removeLast() == Block(expression)) { "Invalid stack" }
        }

        private fun currentStackElement() = stack.lastOrNull()

        override fun visitValueAccess(expression: IrValueAccessExpression) {
            val valueDeclaration = expression.symbol.owner
            if (valueDeclaration is IrVariable && valueDeclaration in variables) {
                variableUsages.getOrPut(currentStackElement()!!) { mutableSetOf() }.add(valueDeclaration)
            }
            super.visitValueAccess(expression)
        }
    })

    fun dfs(currentBlock: BlockOrBody, variable: IrVariable): BlockOrBody? {
        if (variable in (variableUsages[currentBlock] ?: listOf())) return currentBlock
        val childrenResult = childrenBlocks[currentBlock]?.mapNotNull { dfs(it, variable) } ?: listOf()
        return when (childrenResult.size) {
            0 -> return null
            1 -> return childrenResult.single()
            else -> currentBlock
        }
    }

    return variables.associateWith { dfs(body, it) }
}

private fun IrStatement.containsUsagesOf(variablesSet: Set<IrVariable>): Boolean {
    var used = false
    acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (!used) {
                element.acceptChildrenVoid(this)
            }
        }

        override fun visitValueAccess(expression: IrValueAccessExpression) {
            if (expression.symbol.owner in variablesSet) {
                used = true
            }
            super.visitValueAccess(expression)
        }
    })
    return used
}

private fun IrBody.makeBodyWithAddedVariables(context: JvmBackendContext, variables: Set<IrVariable>, symbol: IrSymbol) =
    BlockOrBody.Body(this).makeBodyWithAddedVariables(context, variables, symbol) as IrBody

/**
 * Adds declarations of the variables to the most narrow possible block or body.
 * It adds them before the first usage within the block and inlines initialization of them when possible.
 */
private fun BlockOrBody.makeBodyWithAddedVariables(context: JvmBackendContext, variables: Set<IrVariable>, symbol: IrSymbol): IrElement {
    if (variables.isEmpty()) return element
    extractVariablesSettersToOuterPossibleBlock(variables)
    val nearestBlocks = findNearestBlocksForVariables(variables, this)
    val containingVariables: Map<BlockOrBody, List<IrVariable>> = nearestBlocks.entries
        .mapNotNull { (k, v) -> if (v != null) k to v else null }
        .groupBy({ (_, v) -> v }, { (k, _) -> k })
    return element.transform(object : IrElementTransformerVoid() {
        private fun getFirstInnerStatement(statement: IrStatement): IrStatement? =
            if (statement is IrStatementContainer) statement.statements.first().let(::getFirstInnerStatement) else statement

        private fun removeFirstInnerStatement(statement: IrStatement): IrStatement? {
            if (statement !is IrStatementContainer) return null
            val innerResult = removeFirstInnerStatement(statement.statements[0])
            return when {
                innerResult != null -> statement.also { statement.statements[0] = innerResult }
                statement.statements.size > 1 -> statement.also { statement.statements.removeAt(0) }
                else -> null
            }
        }

        private fun IrStatement.removeInnerEmptyBlocks() {
            if (this !is IrContainerExpression || statements.isEmpty()) return
            val emptyBlocks = statements.mapNotNull {
                it.removeInnerEmptyBlocks()
                if (it is IrContainerExpression && it.statements.isEmpty()) it else null
            }
            statements.removeAll(emptyBlocks)
        }

        private fun replaceSetVariableWithInitialization(variables: List<IrVariable>, container: IrStatementContainer) {
            require(variables.all { it.initializer == null }) { "Variables must have no initializer" }
            val variableFirstUsage = variables.associateWith { v -> container.statements.firstOrNull { it.containsUsagesOf(setOf(v)) } }
            val variableDeclarationPerStatement = variableFirstUsage.entries
                .mapNotNull { (variable, firstUsage) -> if (firstUsage == null) null else firstUsage to variable }
                .groupBy({ (k, _) -> k }, { (_, v) -> v })
            if (variableDeclarationPerStatement.isEmpty()) return
            val newStatements = buildList {
                for (statement in container.statements) {
                    statement.removeInnerEmptyBlocks()
                    if (statement is IrContainerExpression && statement.statements.isEmpty()) continue
                    val varsBefore = variableDeclarationPerStatement[statement]
                    if (varsBefore != null) {
                        addAll(varsBefore)
                        val innerStatement = getFirstInnerStatement(statement)
                        if (innerStatement is IrSetValue) {
                            val assignedVariable = innerStatement.symbol.owner
                            if (assignedVariable is IrVariable && assignedVariable in varsBefore && assignedVariable.initializer == null) {
                                assignedVariable.initializer = innerStatement.value
                                addIfNotNull(removeFirstInnerStatement(statement))
                                continue
                            }
                        }
                    }
                    add(statement)
                }
            }
            container.statements.replaceAll(newStatements)
        }

        override fun visitBlock(expression: IrBlock): IrExpression {
            containingVariables[Block(expression)]?.let {
                expression.transformChildrenVoid()
                replaceSetVariableWithInitialization(it, expression)
                return expression
            }
            return super.visitBlock(expression)
        }

        override fun visitBlockBody(body: IrBlockBody): IrBody {
            containingVariables[BlockOrBody.Body(body)]?.let {
                body.transformChildrenVoid()
                replaceSetVariableWithInitialization(it, body)
                return body
            }
            return super.visitBlockBody(body)
        }

        override fun visitExpressionBody(body: IrExpressionBody): IrBody {
            val lowering = this
            containingVariables[BlockOrBody.Body(body)]?.takeIf { it.isNotEmpty() }?.let { bodyVars ->
                return with(context.createJvmIrBuilder(symbol)) {
                    val blockBody = irBlock { +body.expression.transform(lowering, null) }
                    replaceSetVariableWithInitialization(bodyVars, blockBody)
                    irExprBody(blockBody)
                }
            }
            return super.visitExpressionBody(body)
        }
    }, null)
}

private fun BlockOrBody.extractVariablesSettersToOuterPossibleBlock(variables: Set<IrVariable>) {
    element.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitContainerExpression(expression: IrContainerExpression) {
            super.visitContainerExpression(expression)
            visitStatementContainer(expression)
        }

        override fun visitBlockBody(body: IrBlockBody) {
            super.visitBlockBody(body)
            visitStatementContainer(body)
        }

        private fun visitStatementContainer(container: IrStatementContainer) {
            val newStatements = buildList {
                for (statement in container.statements) {
                    if (statement is IrStatementContainer) {
                        val extracted = statement.statements.takeWhile { it is IrSetValue && it.symbol.owner in variables }
                        if (extracted.isNotEmpty()) {
                            statement.statements.replaceAll(statement.statements.drop(extracted.size))
                            addAll(extracted)
                        }
                        if (statement.statements.isEmpty()) continue
                    }
                    add(statement)
                }
            }
            container.statements.replaceAll(newStatements)
        }
    })
}

private fun <T> MutableList<T>.replaceAll(replacement: List<T>) {
    clear()
    addAll(replacement)
}
