/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.DumpIrTreeWithDescriptorsVisitor
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import java.io.StringWriter


fun ir2string(ir: IrElement?): String = ir2stringWhole(ir).takeWhile { it != '\n' }

fun ir2stringWhole(ir: IrElement?, withDescriptors: Boolean = false): String {
    val strWriter = StringWriter()

    if (withDescriptors)
        ir?.accept(DumpIrTreeWithDescriptorsVisitor(strWriter), "")
    else
        ir?.accept(DumpIrTreeVisitor(strWriter), "")
    return strWriter.toString()
}

fun IrClass.addSimpleDelegatingConstructor(
    superConstructor: IrConstructor,
    irBuiltIns: IrBuiltIns,
    isPrimary: Boolean = false,
    origin: IrDeclarationOrigin? = null
) = WrappedClassConstructorDescriptor().let { descriptor ->
    IrConstructorImpl(
        startOffset, endOffset,
        origin ?: this.origin,
        IrConstructorSymbolImpl(descriptor),
        superConstructor.name,
        superConstructor.visibility,
        defaultType,
        false,
        false,
        isPrimary
    ).also { constructor ->
        descriptor.bind(constructor)
        constructor.parent = this
        declarations += constructor

        superConstructor.valueParameters.mapIndexedTo(constructor.valueParameters) { index, parameter ->
            parameter.copyTo(constructor, index = index)
        }

        constructor.body = IrBlockBodyImpl(
            startOffset, endOffset,
            listOf(
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset, irBuiltIns.unitType,
                    superConstructor.symbol, superConstructor.descriptor,
                    0, superConstructor.valueParameters.size
                ).apply {
                    constructor.valueParameters.forEachIndexed { idx, parameter ->
                        putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol))
                    }
                },
                IrInstanceInitializerCallImpl(startOffset, endOffset, this.symbol, irBuiltIns.unitType)
            )
        )
    }
}

val IrCall.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true
val IrFunctionReference.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != Visibilities.PRIVATE && modality != Modality.FINAL && (parent as? IrClass)?.isFinalClass != true

val IrSimpleFunction.isOverridableOrOverrides: Boolean get() = isOverridable || overriddenSymbols.isNotEmpty()

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

// For an annotation, get the annotation class.
fun IrCall.getAnnotationClass(): IrClass {
    val callable = symbol.owner
    assert(callable is IrConstructor) { "Constructor call expected, got ${ir2string(this)}" }
    val annotationClass =  callable.parentAsClass
    assert(annotationClass.isAnnotationClass) { "Annotation class expected, got ${ir2string(annotationClass)}" }
    return annotationClass
}

val IrTypeParametersContainer.classIfConstructor get() = if (this is IrConstructor) parentAsClass else this

fun IrValueParameter.copyTo(
    irFunction: IrFunction,
    origin: IrDeclarationOrigin = this.origin,
    index: Int = this.index,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = this.name,
    type: IrType = this.type.remapTypeParameters(
            (parent as IrTypeParametersContainer).classIfConstructor,
            irFunction.classIfConstructor
    ),
    varargElementType: IrType? = this.varargElementType,
    defaultValue: IrExpressionBody? = this.defaultValue,
    isCrossinline: Boolean = this.isCrossinline,
    isNoinline: Boolean = this.isNoinline
): IrValueParameter {
    val descriptor = WrappedValueParameterDescriptor(symbol.descriptor.annotations, symbol.descriptor.source)
    val symbol = IrValueParameterSymbolImpl(descriptor)
    val defaultValueCopy = defaultValue?.deepCopyWithVariables()
    defaultValueCopy?.patchDeclarationParents(irFunction)
    return IrValueParameterImpl(
        startOffset, endOffset, origin, symbol,
        name, index, type, varargElementType, isCrossinline, isNoinline
    ).also {
        descriptor.bind(it)
        it.parent = irFunction
        it.defaultValue = defaultValueCopy
        it.annotations.addAll(annotations.map { it.deepCopyWithSymbols() })
    }
}

fun IrTypeParameter.copyToWithoutSuperTypes(
    target: IrTypeParametersContainer,
    shift: Int = 0,
    origin: IrDeclarationOrigin = this.origin
): IrTypeParameter {
    val descriptor = WrappedTypeParameterDescriptor(symbol.descriptor.annotations, symbol.descriptor.source)
    val symbol = IrTypeParameterSymbolImpl(descriptor)
    return IrTypeParameterImpl(startOffset, endOffset, origin, symbol, name, shift + index, isReified, variance).also { copied ->
        descriptor.bind(copied)
        copied.parent = target
    }
}

fun IrFunction.copyValueParametersFrom(from: IrFunction) {
    // TODO: should dispatch receiver be copied?
    dispatchReceiverParameter = from.dispatchReceiverParameter?.let {
        IrValueParameterImpl(it.startOffset, it.endOffset, it.origin, it.descriptor, it.type, it.varargElementType).also {
            it.parent = this
        }
    }
    extensionReceiverParameter = from.extensionReceiverParameter?.copyTo(this)

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
    origin: IrDeclarationOrigin? = null
) {
    val shift = typeParameters.size
    // Any type parameter can figure in a boundary type for any other parameter.
    // Therefore, we first copy the parameters themselves, then set up their supertypes.
    srcTypeParameters.forEachIndexed { i, sourceParameter ->
        assert(sourceParameter.index == i)
        typeParameters.add(sourceParameter.copyToWithoutSuperTypes(this, shift = shift, origin = origin ?: sourceParameter.origin))
    }
    srcTypeParameters.zip(typeParameters.drop(shift)).forEach { (srcParameter, dstParameter) ->
        dstParameter.copySuperTypesFrom(srcParameter)
    }
}

fun IrTypeParametersContainer.copyTypeParametersFrom(
    source: IrTypeParametersContainer,
    origin: IrDeclarationOrigin? = null
) = copyTypeParameters(source.typeParameters, origin)

private fun IrTypeParameter.copySuperTypesFrom(source: IrTypeParameter) {
    val target = this
    val sourceParent = source.parent as IrTypeParametersContainer
    val targetParent = target.parent as IrTypeParametersContainer
    val shift = target.index - source.index
    source.superTypes.forEach {
        target.superTypes.add(it.remapTypeParameters(sourceParent, targetParent, shift))
    }
}

// Copy value parameters, dispatch receiver, and extension receiver from source to value parameters of this function.
// Type of dispatch receiver defaults to source's dispatch receiver. It is overridable in case the new function and the old one are used in
// different contexts and expect different type of dispatch receivers. The overriding type should be assign compatible to the old type.
fun IrFunction.copyValueParametersToStatic(
    source: IrFunction,
    origin: IrDeclarationOrigin,
    dispatchReceiverType: IrType? = source.dispatchReceiverParameter?.type
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

        target.valueParameters.add(
            originalDispatchReceiver.copyTo(
                target,
                origin = originalDispatchReceiver.origin,
                index = shift++,
                type = type,
                name = Name.identifier("\$this")
            )
        )
    }
    source.extensionReceiverParameter?.let { originalExtensionReceiver ->
        target.valueParameters.add(
            originalExtensionReceiver.copyTo(
                target,
                origin = originalExtensionReceiver.origin,
                index = shift++,
                name = Name.identifier("\$receiver")
            )
        )
    }

    for (oldValueParameter in source.valueParameters) {
        target.valueParameters.add(
            oldValueParameter.copyTo(
                target,
                origin = origin,
                index = oldValueParameter.index + shift
            )
        )
    }
}

fun IrFunctionAccessExpression.passTypeArgumentsFrom(irFunction: IrTypeParametersContainer, offset: Int = 0) {
    irFunction.typeParameters.forEachIndexed { i, param ->
        putTypeArgument(i + offset, param.defaultType)
    }
}

/*
    Type parameters should correspond to the function where they are defined.
    `source` is where the type is originally taken from.
 */
fun IrType.remapTypeParameters(source: IrTypeParametersContainer, target: IrTypeParametersContainer, shift: Int = 0): IrType =
    when (this) {
        is IrSimpleType -> {
            val classifier = classifier.owner
            when {
                classifier is IrTypeParameter && classifier.parent == source ->
                    target.typeParameters[classifier.index + shift].defaultType

                classifier is IrClass ->
                    IrSimpleTypeImpl(
                        classifier.symbol,
                        hasQuestionMark,
                        arguments.map {
                            when (it) {
                                is IrTypeProjection -> makeTypeProjection(
                                    it.type.remapTypeParameters(source, target, shift),
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
    this.declarations += declaration
    declaration.accept(SetDeclarationsParentVisitor, this)
}

object SetDeclarationsParentVisitor : IrElementVisitor<Unit, IrDeclarationParent> {
    override fun visitElement(element: IrElement, data: IrDeclarationParent) {
        if (element !is IrDeclarationParent) {
            element.acceptChildren(this, data)
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent) {
        declaration.parent = data
        super.visitDeclaration(declaration, data)
    }
}


val IrFunction.isStatic: Boolean
    get() = parent is IrClass && dispatchReceiverParameter == null

val IrDeclaration.isTopLevel: Boolean
    get() = parent is IrPackageFragment


fun Scope.createTemporaryVariableWithWrappedDescriptor(
    irExpression: IrExpression,
    nameHint: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE): IrVariable {

    val descriptor = WrappedVariableDescriptor()
    return createTemporaryVariableWithGivenDescriptor(
        irExpression, nameHint, isMutable, origin, descriptor
    ).apply { descriptor.bind(this) }
}

fun IrClass.createImplicitParameterDeclarationWithWrappedDescriptor() {
    val thisReceiverDescriptor = WrappedReceiverParameterDescriptor()
    thisReceiver = IrValueParameterImpl(
        startOffset, endOffset,
        IrDeclarationOrigin.INSTANCE_RECEIVER,
        IrValueParameterSymbolImpl(thisReceiverDescriptor),
        Name.identifier("<this>"),
        index = -1,
        type = this.symbol.typeWith(this.typeParameters.map { it.defaultType }),
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false
    ).also { valueParameter ->
        thisReceiverDescriptor.bind(valueParameter)
        valueParameter.parent = this
    }

    assert(typeParameters.isEmpty())
    assert(descriptor.declaredTypeParameters.isEmpty())
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

val IrClass.primaryConstructor: IrConstructor?
    get() = constructors.singleOrNull(IrConstructor::isPrimary)

fun IrClass.createParameterDeclarations() {
    assert (thisReceiver == null)

    thisReceiver = WrappedReceiverParameterDescriptor().let {
        IrValueParameterImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.INSTANCE_RECEIVER,
            IrValueParameterSymbolImpl(it),
            Name.special("<this>"),
            0,
            symbol.typeWith(typeParameters.map { it.defaultType }),
            null,
            false,
            false
        ).apply {
            it.bind(this)
            parent = this@createParameterDeclarations
        }
    }
}

fun IrFunction.createDispatchReceiverParameter(origin: IrDeclarationOrigin? = null) {
    assert(dispatchReceiverParameter == null)

    dispatchReceiverParameter = IrValueParameterImpl(
        startOffset, endOffset,
        origin ?: parentAsClass.origin,
        IrValueParameterSymbolImpl(parentAsClass.thisReceiver!!.descriptor),
        Name.special("<this>"),
        0,
        parentAsClass.defaultType,
        null,
        false,
        false
    ).apply {
        parent = this@createDispatchReceiverParameter
    }
}

val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        listOf(this.constructedClass.thisReceiver
                   ?: error(this.descriptor)
        ) + explicitParameters
    } else {
        explicitParameters
    }

fun IrClass.addFakeOverrides() {
    fun IrDeclaration.toList() = when (this) {
        is IrSimpleFunction -> listOf(this)
        is IrProperty -> listOfNotNull(getter, setter)
        else -> emptyList()
    }

    val overriddenFunctions = declarations
        .flatMap { it.toList() }
        .flatMap { it.overriddenSymbols.map { it.owner } }
        .toSet()

    val unoverriddenSuperFunctions = superTypes
        .map { it.getClass()!! }
        .flatMap { irClass ->
            irClass.declarations
                .flatMap { it.toList() }
                .filter { it !in overriddenFunctions }
        }
        .toMutableSet()

    // TODO: A dirty hack.
    val groupedUnoverriddenSuperFunctions = unoverriddenSuperFunctions.groupBy { it.name.asString() + it.allParameters.size }

    fun createFakeOverride(overriddenFunctions: List<IrSimpleFunction>) =
        overriddenFunctions.first().let { irFunction ->
            val descriptor = WrappedSimpleFunctionDescriptor()
            IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                IrDeclarationOrigin.FAKE_OVERRIDE,
                IrSimpleFunctionSymbolImpl(descriptor),
                irFunction.name,
                Visibilities.INHERITED,
                Modality.OPEN,
                irFunction.returnType,
                irFunction.isInline,
                irFunction.isExternal,
                irFunction.isTailrec,
                irFunction.isSuspend
            ).apply {
                descriptor.bind(this)
                parent = this@addFakeOverrides
                overriddenSymbols += overriddenFunctions.map { it.symbol }
                copyParameterDeclarationsFrom(irFunction)
            }
        }

    val fakeOverriddenFunctions = groupedUnoverriddenSuperFunctions
        .asSequence()
        .associate { it.value.first() to createFakeOverride(it.value) }
        .toMutableMap()

    declarations += fakeOverriddenFunctions.values
}
