/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildTypeParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrOverridingUtil
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.io.StringWriter

fun ir2string(ir: IrElement?): String = ir?.render() ?: ""

@Suppress("unused") // Used in kotlin-native
fun ir2stringWhole(ir: IrElement?): String {
    val strWriter = StringWriter()
    ir?.accept(DumpIrTreeVisitor(strWriter), "")
    return strWriter.toString()
}

@Suppress("DEPRECATION_ERROR")
@Deprecated("rhizomedb & noria compatibility", level = DeprecationLevel.ERROR)
fun IrClass.addSimpleDelegatingConstructor(
    superConstructor: IrConstructor,
    irBuiltIns: org.jetbrains.kotlin.ir.descriptors.IrBuiltIns,
    isPrimary: Boolean = false,
    origin: IrDeclarationOrigin? = null
): IrConstructor = addSimpleDelegatingConstructor(superConstructor, irBuiltIns.irBuiltIns, isPrimary, origin)

fun IrClass.addSimpleDelegatingConstructor(
    superConstructor: IrConstructor,
    irBuiltIns: IrBuiltIns,
    isPrimary: Boolean = false,
    origin: IrDeclarationOrigin? = null
): IrConstructor =
    addConstructor {
        val klass = this@addSimpleDelegatingConstructor
        this.startOffset = klass.startOffset
        this.endOffset = klass.endOffset
        this.origin = origin ?: klass.origin
        this.visibility = superConstructor.visibility
        this.isPrimary = isPrimary
    }.also { constructor ->
        constructor.valueParameters = superConstructor.valueParameters.mapIndexed { index, parameter ->
            parameter.copyTo(constructor, index = index)
        }

        constructor.body = factory.createBlockBody(
            startOffset, endOffset,
            listOf(
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset, irBuiltIns.unitType,
                    superConstructor.symbol, 0,
                    superConstructor.valueParameters.size
                ).apply {
                    constructor.valueParameters.forEachIndexed { idx, parameter ->
                        putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol))
                    }
                },
                IrInstanceInitializerCallImpl(startOffset, endOffset, this.symbol, irBuiltIns.unitType)
            )
        )
    }

val IrCall.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true
val IrFunctionReference.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != DescriptorVisibilities.PRIVATE && modality != Modality.FINAL && (parent as? IrClass)?.isFinalClass != true

val IrSimpleFunction.isOverridableOrOverrides: Boolean get() = isOverridable || overriddenSymbols.isNotEmpty()

val IrDeclaration.isMemberOfOpenClass: Boolean
    get() {
        val parentClass = this.parent as? IrClass ?: return false
        return !parentClass.isFinalClass
    }

fun IrReturnTarget.returnType(context: CommonBackendContext) =
    when (this) {
        is IrConstructor -> context.irBuiltIns.unitType
        is IrFunction -> returnType
        is IrReturnableBlock -> type
        else -> error("Unknown ReturnTarget: $this")
    }

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

val IrTypeParametersContainer.classIfConstructor get() = if (this is IrConstructor) parentAsClass else this

fun IrValueParameter.copyTo(
    irFunction: IrFunction,
    origin: IrDeclarationOrigin = this.origin,
    index: Int = this.index,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = this.name,
    remapTypeMap: Map<IrTypeParameter, IrTypeParameter> = mapOf(),
    type: IrType = this.type.remapTypeParameters(
        (parent as IrTypeParametersContainer).classIfConstructor,
        irFunction.classIfConstructor,
        remapTypeMap
    ),
    varargElementType: IrType? = this.varargElementType, // TODO: remapTypeParameters here as well
    defaultValue: IrExpressionBody? = this.defaultValue,
    isCrossinline: Boolean = this.isCrossinline,
    isNoinline: Boolean = this.isNoinline,
    isAssignable: Boolean = this.isAssignable
): IrValueParameter {
    val symbol = IrValueParameterSymbolImpl()
    val defaultValueCopy = defaultValue?.let { originalDefault ->
        factory.createExpressionBody(originalDefault.startOffset, originalDefault.endOffset) {
            expression = originalDefault.expression.deepCopyWithVariables().also {
                it.patchDeclarationParents(irFunction)
            }
        }
    }
    return factory.createValueParameter(
        startOffset, endOffset, origin, symbol,
        name, index, type, varargElementType, isCrossinline = isCrossinline,
        isNoinline = isNoinline, isHidden = false, isAssignable = isAssignable
    ).also {
        it.parent = irFunction
        it.defaultValue = defaultValueCopy
        it.copyAnnotationsFrom(this)
    }
}

fun IrTypeParameter.copyToWithoutSuperTypes(
    target: IrTypeParametersContainer,
    index: Int = this.index,
    origin: IrDeclarationOrigin = this.origin
): IrTypeParameter = buildTypeParameter(target) {
    updateFrom(this@copyToWithoutSuperTypes)
    this.name = this@copyToWithoutSuperTypes.name
    this.origin = origin
    this.index = index
}

fun IrFunction.copyReceiverParametersFrom(from: IrFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
    dispatchReceiverParameter = from.dispatchReceiverParameter?.run {
        factory.createValueParameter(
            startOffset, endOffset, origin,
            IrValueParameterSymbolImpl(),
            name, index,
            type.substitute(substitutionMap),
            varargElementType?.substitute(substitutionMap),
            isCrossinline, isNoinline,
            isHidden, isAssignable
        ).also { parameter ->
            parameter.parent = this@copyReceiverParametersFrom
        }
    }
    extensionReceiverParameter = from.extensionReceiverParameter?.copyTo(this)
}

fun IrFunction.copyValueParametersFrom(from: IrFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
    copyReceiverParametersFrom(from, substitutionMap)
    val shift = valueParameters.size
    valueParameters += from.valueParameters.map {
        it.copyTo(this, index = it.index + shift, type = it.type.substitute(substitutionMap))
    }
}

fun IrFunction.copyParameterDeclarationsFrom(from: IrFunction) {
    assert(typeParameters.isEmpty())
    copyTypeParametersFrom(from)
    val substitutionMap = makeTypeParameterSubstitutionMap(from, this)
    copyValueParametersFrom(from, substitutionMap)
}

fun IrTypeParametersContainer.copyTypeParameters(
    srcTypeParameters: List<IrTypeParameter>,
    origin: IrDeclarationOrigin? = null,
    parameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): List<IrTypeParameter> {
    val shift = typeParameters.size
    val oldToNewParameterMap = parameterMap.orEmpty().toMutableMap()
    // Any type parameter can figure in a boundary type for any other parameter.
    // Therefore, we first copy the parameters themselves, then set up their supertypes.
    val newTypeParameters = srcTypeParameters.mapIndexed { i, sourceParameter ->
        sourceParameter.copyToWithoutSuperTypes(this, index = i + shift, origin = origin ?: sourceParameter.origin).also {
            oldToNewParameterMap[sourceParameter] = it
        }
    }
    typeParameters += newTypeParameters
    srcTypeParameters.zip(newTypeParameters).forEach { (srcParameter, dstParameter) ->
        dstParameter.copySuperTypesFrom(srcParameter, oldToNewParameterMap)
    }
    return newTypeParameters
}

fun IrTypeParametersContainer.copyTypeParametersFrom(
    source: IrTypeParametersContainer,
    origin: IrDeclarationOrigin? = null,
    parameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
) = copyTypeParameters(source.typeParameters, origin, parameterMap)

private fun IrTypeParameter.copySuperTypesFrom(source: IrTypeParameter, srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>) {
    val target = this
    val sourceParent = source.parent as IrTypeParametersContainer
    val targetParent = target.parent as IrTypeParametersContainer
    target.superTypes = source.superTypes.map {
        it.remapTypeParameters(sourceParent, targetParent, srcToDstParameterMap)
    }
}

fun IrAnnotationContainer.copyAnnotations(): List<IrConstructorCall> {
    return annotations.map { it.deepCopyWithSymbols(this as? IrDeclarationParent) }
}

fun IrAnnotationContainer.copyAnnotationsWhen(filter: IrConstructorCall.() -> Boolean): List<IrConstructorCall> {
    return annotations.mapNotNull { if (it.filter()) it.deepCopyWithSymbols(this as? IrDeclarationParent) else null }
}

fun IrMutableAnnotationContainer.copyAnnotationsFrom(source: IrAnnotationContainer) {
    annotations += source.copyAnnotations()
}

fun makeTypeParameterSubstitutionMap(
    original: IrTypeParametersContainer,
    transformed: IrTypeParametersContainer
): Map<IrTypeParameterSymbol, IrType> =
    original.typeParameters
        .map { it.symbol }
        .zip(transformed.typeParameters.map { it.defaultType })
        .toMap()


// Copy value parameters, dispatch receiver, and extension receiver from source to value parameters of this function.
// Type of dispatch receiver defaults to source's dispatch receiver. It is overridable in case the new function and the old one are used in
// different contexts and expect different type of dispatch receivers. The overriding type should be assign compatible to the old type.
fun IrFunction.copyValueParametersToStatic(
    source: IrFunction,
    origin: IrDeclarationOrigin,
    dispatchReceiverType: IrType? = source.dispatchReceiverParameter?.type,
    numValueParametersToCopy: Int = source.valueParameters.size
) {
    val target = this
    assert(target.valueParameters.isEmpty())

    var shift = 0
    source.dispatchReceiverParameter?.let { originalDispatchReceiver ->
        assert(dispatchReceiverType!!.isSubtypeOfClass(originalDispatchReceiver.type.classOrNull!!))
        val type = dispatchReceiverType.remapTypeParameters(
            (originalDispatchReceiver.parent as IrTypeParametersContainer).classIfConstructor,
            target.classIfConstructor
        )

        target.valueParameters += originalDispatchReceiver.copyTo(
            target,
            origin = originalDispatchReceiver.origin,
            index = shift++,
            type = type,
            name = Name.identifier("\$this")
        )
    }
    source.extensionReceiverParameter?.let { originalExtensionReceiver ->
        target.valueParameters += originalExtensionReceiver.copyTo(
            target,
            origin = originalExtensionReceiver.origin,
            index = shift++,
            name = Name.identifier("\$receiver")
        )
    }

    for (oldValueParameter in source.valueParameters) {
        if (oldValueParameter.index >= numValueParametersToCopy) break
        target.valueParameters += oldValueParameter.copyTo(
            target,
            origin = origin,
            index = oldValueParameter.index + shift
        )
    }
}

fun IrFunctionAccessExpression.passTypeArgumentsFrom(irFunction: IrTypeParametersContainer, offset: Int = 0) {
    irFunction.typeParameters.forEachIndexed { i, param ->
        putTypeArgument(i + offset, param.defaultType)
    }
}

/**
 * Perform a substitution of type parameters occuring in [this]. In order of
 * precedence, parameter `P` is substituted with...
 *
 *   1) `T`, if `srcToDstParameterMap.get(P) == T`
 *   2) `T`, if `source.typeParameters[i] == P` and
 *      `target.typeParameters[i] == T`
 *   3) `P`
 *
 *  If [srcToDstParameterMap] is total on the domain of type parameters in
 *  [this], this effectively performs a substitution according to that map.
 */
fun IrType.remapTypeParameters(
    source: IrTypeParametersContainer,
    target: IrTypeParametersContainer,
    srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): IrType =
    when (this) {
        is IrSimpleType -> {
            val classifier = classifier.owner
            when {
                classifier is IrTypeParameter -> {
                    val newClassifier =
                        srcToDstParameterMap?.get(classifier) ?: if (classifier.parent == source)
                            target.typeParameters[classifier.index]
                        else
                            classifier
                    IrSimpleTypeImpl(newClassifier.symbol, hasQuestionMark, arguments, annotations)
                }

                classifier is IrClass ->
                    IrSimpleTypeImpl(
                        classifier.symbol,
                        hasQuestionMark,
                        arguments.map {
                            when (it) {
                                is IrTypeProjection -> makeTypeProjection(
                                    it.type.remapTypeParameters(source, target, srcToDstParameterMap),
                                    it.variance
                                )
                                else -> it
                            }
                        },
                        annotations
                    )

                else -> this
            }
        }
        else -> this
    }

/* Copied from K/N */
fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    declaration.factory.stageController.unrestrictDeclarationListsAccess {
        this.declarations += declaration
    }
    declaration.setDeclarationsParent(this)
}

fun <T : IrElement> T.setDeclarationsParent(parent: IrDeclarationParent): T {
    accept(SetDeclarationsParentVisitor, parent)
    return this
}

object SetDeclarationsParentVisitor : IrElementVisitor<Unit, IrDeclarationParent> {
    override fun visitElement(element: IrElement, data: IrDeclarationParent) {
        if (element !is IrDeclarationParent) {
            element.acceptChildren(this, data)
        }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent) {
        declaration.parent = data
        super.visitDeclaration(declaration, data)
    }
}


val IrFunction.isStatic: Boolean
    get() = parent is IrClass && dispatchReceiverParameter == null

val IrDeclaration.isTopLevel: Boolean
    get() {
        if (parent is IrPackageFragment) return true
        val parentClass = parent as? IrClass
        return parentClass?.isFileClass == true && parentClass.parent is IrPackageFragment
    }

fun IrClass.createImplicitParameterDeclarationWithWrappedDescriptor() {
    thisReceiver = buildReceiverParameter(this, IrDeclarationOrigin.INSTANCE_RECEIVER, symbol.typeWithParameters(typeParameters))
}

@Suppress("UNCHECKED_CAST")
fun isElseBranch(branch: IrBranch) = branch is IrElseBranch || ((branch.condition as? IrConst<Boolean>)?.value == true)

fun IrFunction.isMethodOfAny(): Boolean =
    extensionReceiverParameter == null && dispatchReceiverParameter != null &&
            when (name) {
                OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING -> valueParameters.isEmpty()
                OperatorNameConventions.EQUALS -> valueParameters.singleOrNull()?.type?.isNullableAny() == true
                else -> false
            }

fun IrDeclarationContainer.simpleFunctions() = declarations.flatMap {
    when (it) {
        is IrSimpleFunction -> listOf(it)
        is IrProperty -> listOfNotNull(it.getter, it.setter)
        else -> emptyList()
    }
}


fun IrClass.createParameterDeclarations() {
    assert(thisReceiver == null)
    thisReceiver = buildReceiverParameter(this, IrDeclarationOrigin.INSTANCE_RECEIVER, symbol.typeWithParameters(typeParameters))
}

fun IrFunction.createDispatchReceiverParameter(origin: IrDeclarationOrigin? = null) {
    assert(dispatchReceiverParameter == null)

    dispatchReceiverParameter = factory.createValueParameter(
        startOffset, endOffset,
        origin ?: parentAsClass.origin,
        IrValueParameterSymbolImpl(),
        SpecialNames.THIS,
        -1,
        parentAsClass.defaultType,
        null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
        isAssignable = false
    ).apply {
        parent = this@createDispatchReceiverParameter
    }
}

val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        ArrayList<IrValueParameter>(allParametersCount).also {
            it.add(
                this.constructedClass.thisReceiver
                    ?: error(this.render())
            )
            addExplicitParametersTo(it)
        }
    } else {
        explicitParameters
    }

val IrFunction.allParametersCount: Int
    get() = if (this is IrConstructor) explicitParametersCount + 1 else explicitParametersCount

// This is essentially the same as FakeOverrideBuilder,
// but it bypasses SymbolTable.
// TODO: merge it with FakeOverrideBuilder.
private class FakeOverrideBuilderForLowerings : FakeOverrideBuilderStrategy(emptyMap()) {

    override fun linkFunctionFakeOverride(declaration: IrFakeOverrideFunction, compatibilityMode: Boolean) {
        declaration.acquireSymbol(IrSimpleFunctionSymbolImpl())
    }

    override fun linkPropertyFakeOverride(declaration: IrFakeOverrideProperty, compatibilityMode: Boolean) {
        val propertySymbol = IrPropertySymbolImpl()
        declaration.getter?.let { it.correspondingPropertySymbol = propertySymbol }
        declaration.setter?.let { it.correspondingPropertySymbol = propertySymbol }

        declaration.acquireSymbol(propertySymbol)

        declaration.getter?.let {
            it.correspondingPropertySymbol = declaration.symbol
            linkFunctionFakeOverride(it as? IrFakeOverrideFunction ?: error("Unexpected fake override getter: $it"), compatibilityMode)
        }
        declaration.setter?.let {
            it.correspondingPropertySymbol = declaration.symbol
            linkFunctionFakeOverride(it as? IrFakeOverrideFunction ?: error("Unexpected fake override setter: $it"), compatibilityMode)
        }
    }
}

fun IrClass.addFakeOverrides(typeSystem: IrTypeSystemContext, implementedMembers: List<IrOverridableMember> = emptyList()) {
    IrOverridingUtil(typeSystem, FakeOverrideBuilderForLowerings())
        .buildFakeOverridesForClassUsingOverriddenSymbols(this, implementedMembers, compatibilityMode = false)
        .forEach { addChild(it) }
}

@Suppress("DEPRECATION_ERROR")
@Deprecated("rhizomedb & noria compatibility", level = DeprecationLevel.ERROR)
fun IrClass.addFakeOverrides(
    irBuiltIns: org.jetbrains.kotlin.ir.descriptors.IrBuiltIns,
    implementedMembers: List<IrOverridableMember> = emptyList()
) {
    addFakeOverrides(IrTypeSystemContextImpl(irBuiltIns.irBuiltIns), implementedMembers)
}

fun IrFactory.createStaticFunctionWithReceivers(
    irParent: IrDeclarationParent,
    name: Name,
    oldFunction: IrFunction,
    dispatchReceiverType: IrType? = oldFunction.dispatchReceiverParameter?.type,
    origin: IrDeclarationOrigin = oldFunction.origin,
    modality: Modality = Modality.FINAL,
    visibility: DescriptorVisibility = oldFunction.visibility,
    isFakeOverride: Boolean = oldFunction.isFakeOverride,
    copyMetadata: Boolean = true,
    typeParametersFromContext: List<IrTypeParameter> = listOf()
): IrSimpleFunction {
    return createFunction(
        oldFunction.startOffset, oldFunction.endOffset,
        origin,
        IrSimpleFunctionSymbolImpl(),
        name,
        visibility,
        modality,
        oldFunction.returnType,
        isInline = oldFunction.isInline,
        isExternal = false,
        isTailrec = false,
        isSuspend = oldFunction.isSuspend,
        isExpect = oldFunction.isExpect,
        isFakeOverride = isFakeOverride,
        isOperator = oldFunction is IrSimpleFunction && oldFunction.isOperator,
        isInfix = oldFunction is IrSimpleFunction && oldFunction.isInfix,
        containerSource = oldFunction.containerSource,
    ).apply {
        parent = irParent

        val newTypeParametersFromContext = copyAndRenameConflictingTypeParametersFrom(
            typeParametersFromContext,
            oldFunction.typeParameters
        )
        val newTypeParametersFromFunction = copyTypeParametersFrom(oldFunction)
        val typeParameterMap =
            (typeParametersFromContext + oldFunction.typeParameters)
                .zip(newTypeParametersFromContext + newTypeParametersFromFunction).toMap()

        fun remap(type: IrType): IrType =
            type.remapTypeParameters(oldFunction, this, typeParameterMap)

        typeParameters.forEach { it.superTypes = it.superTypes.map(::remap) }

        annotations = oldFunction.annotations

        var offset = 0
        val dispatchReceiver = oldFunction.dispatchReceiverParameter?.copyTo(
            this,
            name = Name.identifier("this"),
            index = offset++,
            type = remap(dispatchReceiverType!!),
            origin = IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER
        )
        val extensionReceiver = oldFunction.extensionReceiverParameter?.copyTo(
            this,
            name = Name.identifier("receiver"),
            index = offset++,
            origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER,
            remapTypeMap = typeParameterMap
        )
        valueParameters = listOfNotNull(dispatchReceiver, extensionReceiver) +
                oldFunction.valueParameters.map {
                    it.copyTo(
                        this,
                        index = it.index + offset,
                        remapTypeMap = typeParameterMap
                    )
                }

        if (copyMetadata) metadata = oldFunction.metadata

        copyAttributes(oldFunction as? IrAttributeContainer)
    }
}

/**
 * Appends the parameters in [contextParameters] to the type parameters of
 * [this] function, renaming those that may clash with a provided collection of
 * [existingParameters] (e.g. type parameters of the function itself, when
 * creating DefaultImpls).
 *
 * @returns List of newly created, possibly renamed, copies of type parameters
 *     in order of the corresponding parameters in [context].
 */
private fun IrSimpleFunction.copyAndRenameConflictingTypeParametersFrom(
    contextParameters: List<IrTypeParameter>,
    existingParameters: Collection<IrTypeParameter>
): List<IrTypeParameter> {
    val newParameters = mutableListOf<IrTypeParameter>()

    val existingNames =
        (contextParameters.map { it.name.asString() } + existingParameters.map { it.name.asString() }).toMutableSet()

    contextParameters.forEach { contextType ->
        val newName = if (existingParameters.any { it.name.asString() == contextType.name.asString() }) {
            val newNamePrefix = contextType.name.asString() + "_I"
            val newName = newNamePrefix + generateSequence(1) { x -> x + 1 }.first { n ->
                (newNamePrefix + n) !in existingNames
            }
            existingNames.add(newName)
            newName
        } else {
            contextType.name.asString()
        }

        newParameters.add(buildTypeParameter(this) {
            updateFrom(contextType)
            name = Name.identifier(newName)
        })
    }

    val zipped = contextParameters.zip(newParameters)
    val parameterMap = zipped.toMap()
    for ((oldParameter, newParameter) in zipped) {
        newParameter.copySuperTypesFrom(oldParameter, parameterMap)
    }

    typeParameters = typeParameters + newParameters

    return newParameters
}

val IrSymbol.isSuspend: Boolean
    get() = this is IrSimpleFunctionSymbol && owner.isSuspend

fun IrSimpleFunction.allOverridden(includeSelf: Boolean = false): List<IrSimpleFunction> {
    val result = mutableListOf<IrSimpleFunction>()
    if (includeSelf) {
        result.add(this)
    }

    var current = this
    while (true) {
        val overridden = current.overriddenSymbols
        when (overridden.size) {
            0 -> return result
            1 -> {
                current = overridden[0].owner
                result.add(current)
            }
            else -> {
                val resultSet = result.toMutableSet()
                computeAllOverridden(current, resultSet)
                return resultSet.toList()
            }
        }
    }
}

private fun computeAllOverridden(function: IrSimpleFunction, result: MutableSet<IrSimpleFunction>) {
    for (overriddenSymbol in function.overriddenSymbols) {
        val override = overriddenSymbol.owner
        if (result.add(override)) {
            computeAllOverridden(override, result)
        }
    }
}

// TODO: support more cases like built-in operator call and so on
fun IrExpression?.isPure(
    anyVariable: Boolean,
    checkFields: Boolean = true,
    context: CommonBackendContext? = null
): Boolean {
    if (this == null) return true

    fun IrExpression.isPureImpl(): Boolean {
        return when (this) {
            is IrConst<*> -> true
            is IrGetValue -> {
                if (anyVariable) return true
                val valueDeclaration = symbol.owner
                if (valueDeclaration is IrVariable) !valueDeclaration.isVar
                else true
            }
            is IrTypeOperatorCall ->
                (
                        operator == IrTypeOperator.INSTANCEOF ||
                                operator == IrTypeOperator.REINTERPRET_CAST ||
                                operator == IrTypeOperator.NOT_INSTANCEOF
                        ) && argument.isPure(anyVariable, checkFields, context)
            is IrCall -> if (context?.isSideEffectFree(this) == true) {
                for (i in 0 until valueArgumentsCount) {
                    val valueArgument = getValueArgument(i)
                    if (!valueArgument.isPure(anyVariable, checkFields, context)) return false
                }
                true
            } else false
            is IrGetObjectValue -> type.isUnit()
            is IrVararg -> elements.all { (it as? IrExpression)?.isPure(anyVariable, checkFields, context) == true }
            else -> false
        }
    }

    if (isPureImpl()) return true

    if (!checkFields) return false

    if (this is IrGetField) {
        if (!symbol.owner.isFinal) {
            if (!anyVariable) {
                return false
            }
        }
        return receiver.isPure(anyVariable)
    }

    return false
}

fun CommonBackendContext.createArrayOfExpression(
    startOffset: Int, endOffset: Int,
    arrayElementType: IrType,
    arrayElements: List<IrExpression>
): IrExpression {

    val arrayType = ir.symbols.array.typeWith(arrayElementType)
    val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)

    return IrCallImpl(
        startOffset,
        endOffset,
        arrayType,
        ir.symbols.arrayOf,
        1,
        1
    ).apply {
        putTypeArgument(0, arrayElementType)
        putValueArgument(0, arg0)
    }
}

fun IrBuiltIns.getKFunctionType(returnType: IrType, parameterTypes: List<IrType>) =
    kFunctionN(parameterTypes.size).typeWith(parameterTypes + returnType)