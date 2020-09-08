/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildTypeParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrOverridingUtil
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import java.io.StringWriter

fun ir2string(ir: IrElement?): String = ir?.render() ?: ""

@Suppress("unused") // Used in kotlin-native
fun ir2stringWhole(ir: IrElement?): String {
    val strWriter = StringWriter()
    ir?.accept(DumpIrTreeVisitor(strWriter), "")
    return strWriter.toString()
}

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
    isNoinline: Boolean = this.isNoinline
): IrValueParameter {
    val descriptor = if (index < 0) {
        WrappedReceiverParameterDescriptor()
    } else {
        WrappedValueParameterDescriptor()
    }
    val symbol = IrValueParameterSymbolImpl(descriptor)
    val defaultValueCopy = defaultValue?.let { originalDefault ->
        factory.createExpressionBody(originalDefault.startOffset, originalDefault.endOffset) {
            expression = originalDefault.expression.deepCopyWithVariables().also {
                it.patchDeclarationParents(irFunction)
            }
        }
    }
    return factory.createValueParameter(
        startOffset, endOffset, origin, symbol,
        name, index, type, varargElementType, isCrossinline, isNoinline
    ).also {
        descriptor.bind(it)
        it.parent = irFunction
        it.defaultValue = defaultValueCopy
        it.annotations = annotations.map { it.deepCopyWithSymbols() }
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

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrFunction.copyReceiverParametersFrom(from: IrFunction) {
    dispatchReceiverParameter = from.dispatchReceiverParameter?.run {
        val newDescriptor = WrappedReceiverParameterDescriptor()
        factory.createValueParameter(
            startOffset, endOffset, origin,
            IrValueParameterSymbolImpl(newDescriptor),
            name,
            index, type, varargElementType, isCrossinline, isNoinline
        ).also { parameter ->
            parameter.parent = this@copyReceiverParametersFrom
            newDescriptor.bind(this)
        }
    }
    extensionReceiverParameter = from.extensionReceiverParameter?.copyTo(this)
}

fun IrFunction.copyValueParametersFrom(from: IrFunction) {
    copyReceiverParametersFrom(from)
    val shift = valueParameters.size
    valueParameters += from.valueParameters.map { it.copyTo(this, index = it.index + shift) }
}

fun IrFunction.copyParameterDeclarationsFrom(from: IrFunction) {
    assert(typeParameters.isEmpty())
    copyTypeParametersFrom(from)
    copyValueParametersFrom(from)
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
                        srcToDstParameterMap?.get(classifier) ?:
                        if (classifier.parent == source)
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
    stageController.unrestrictDeclarationListsAccess {
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

fun Scope.createTemporaryVariableWithWrappedDescriptor(
    irExpression: IrExpression,
    nameHint: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
): IrVariable {

    val descriptor = WrappedVariableDescriptor()
    return createTemporaryVariableWithGivenDescriptor(
        irExpression, nameHint, isMutable, origin, descriptor
    ).apply { descriptor.bind(this) }
}

fun IrClass.createImplicitParameterDeclarationWithWrappedDescriptor() {
    thisReceiver = buildReceiverParameter(this, IrDeclarationOrigin.INSTANCE_RECEIVER, symbol.typeWithParameters(typeParameters))
}

@Suppress("UNCHECKED_CAST")
fun isElseBranch(branch: IrBranch) = branch is IrElseBranch || ((branch.condition as? IrConst<Boolean>)?.value == true)

fun IrSimpleFunction.isMethodOfAny() =
    ((valueParameters.size == 0 && name.asString().let { it == "hashCode" || it == "toString" }) ||
            (valueParameters.size == 1 && name.asString() == "equals" && valueParameters[0].type.isNullableAny()))

fun IrClass.simpleFunctions() = declarations.flatMap {
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

    val newDescriptor = WrappedReceiverParameterDescriptor()
    dispatchReceiverParameter = factory.createValueParameter(
        startOffset, endOffset,
        origin ?: parentAsClass.origin,
        IrValueParameterSymbolImpl(newDescriptor),
        Name.special("<this>"),
        -1,
        parentAsClass.defaultType,
        null,
        false,
        false
    ).apply {
        parent = this@createDispatchReceiverParameter
        newDescriptor.bind(this)
    }
}

val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        listOf(
            this.constructedClass.thisReceiver
                ?: error(this.render())
        ) + explicitParameters
    } else {
        explicitParameters
    }

private object FakeOverrideBuilder : FakeOverrideBuilderStrategy() {
    override fun linkFakeOverride(fakeOverride: IrOverridableMember) {
        when (fakeOverride) {
            is IrFakeOverrideFunction -> linkFunctionFakeOverride(fakeOverride)
            is IrFakeOverrideProperty -> linkPropertyFakeOverride(fakeOverride)
            else -> error("Unexpected fake override: $fakeOverride")
        }
    }

    private fun linkFunctionFakeOverride(declaration: IrFakeOverrideFunction) {
        declaration.acquireSymbol(IrSimpleFunctionSymbolImpl(WrappedSimpleFunctionDescriptor()))
    }

    private fun linkPropertyFakeOverride(declaration: IrFakeOverrideProperty) {
        val propertySymbol = IrPropertySymbolImpl(WrappedPropertyDescriptor())
        declaration.getter?.let { it.correspondingPropertySymbol = propertySymbol }
        declaration.setter?.let { it.correspondingPropertySymbol = propertySymbol }

        declaration.acquireSymbol(propertySymbol)

        declaration.getter?.let {
            it.correspondingPropertySymbol = declaration.symbol
            linkFunctionFakeOverride(it as? IrFakeOverrideFunction ?: error("Unexpected fake override getter: $it"))
        }
        declaration.setter?.let {
            it.correspondingPropertySymbol = declaration.symbol
            linkFunctionFakeOverride(it as? IrFakeOverrideFunction ?: error("Unexpected fake override setter: $it"))
        }
    }
}

fun IrClass.addFakeOverrides(irBuiltIns: IrBuiltIns, implementedMembers: List<IrOverridableMember> = emptyList()) {
    IrOverridingUtil(irBuiltIns, FakeOverrideBuilder)
        .buildFakeOverridesForClassUsingOverriddenSymbols(this, implementedMembers)
        .forEach { addChild(it) }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
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
    val descriptor = (oldFunction.descriptor as? DescriptorWithContainerSource)?.let {
        WrappedFunctionDescriptorWithContainerSource()
    } ?: WrappedSimpleFunctionDescriptor()
    return createFunction(
        oldFunction.startOffset, oldFunction.endOffset,
        origin,
        IrSimpleFunctionSymbolImpl(descriptor),
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
        descriptor.bind(this)
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

fun IrSimpleFunction.allOverridden(includeSelf: Boolean = false): Set<IrSimpleFunction> {
    val result = mutableSetOf<IrSimpleFunction>()
    if (includeSelf) {
        computeAllOverridden(this, result)
    } else {
        for (override in overriddenSymbols) {
            computeAllOverridden(override.owner, result)
        }
    }
    return result
}

private fun computeAllOverridden(function: IrSimpleFunction, result: MutableSet<IrSimpleFunction>) {
    if (result.add(function)) {
        for (override in function.overriddenSymbols) {
            computeAllOverridden(override.owner, result)
        }
    }
}
