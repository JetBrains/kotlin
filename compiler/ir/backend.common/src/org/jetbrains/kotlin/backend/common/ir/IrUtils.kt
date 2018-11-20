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
import org.jetbrains.kotlin.backend.common.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
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

fun DeclarationDescriptor.createFakeOverrideDescriptor(owner: ClassDescriptor): DeclarationDescriptor? {
    // We need to copy descriptors for vtable building, thus take only functions and properties.
    return when (this) {
        is CallableMemberDescriptor ->
            copy(
                /* newOwner      = */ owner,
                /* modality      = */ modality,
                /* visibility    = */ visibility,
                /* kind          = */ CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                /* copyOverrides = */ true
            ).apply {
                overriddenDescriptors += this@createFakeOverrideDescriptor
            }
        else -> null
    }
}

fun FunctionDescriptor.createOverriddenDescriptor(owner: ClassDescriptor, final: Boolean = true): FunctionDescriptor {
    return this.newCopyBuilder()
        .setOwner(owner)
        .setCopyOverrides(true)
        .setModality(if (final) Modality.FINAL else Modality.OPEN)
        .setDispatchReceiverParameter(owner.thisAsReceiverParameter)
        .build()!!.apply {
        overriddenDescriptors += this@createOverriddenDescriptor
    }
}

fun IrClass.addSimpleDelegatingConstructor(
        superConstructor: IrConstructor,
        irBuiltIns: IrBuiltIns,
        origin: IrDeclarationOrigin,
        isPrimary: Boolean = false
): IrConstructor {
    val superConstructorDescriptor = superConstructor.descriptor
    val constructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            /* containingDeclaration = */ this.descriptor,
            /* annotations           = */ Annotations.EMPTY,
            /* isPrimary             = */ isPrimary,
            /* source                = */ SourceElement.NO_SOURCE
    )
    val valueParameters = superConstructor.valueParameters.map {
        val descriptor = it.descriptor as ValueParameterDescriptor
        val newDescriptor = descriptor.copy(constructorDescriptor, descriptor.name, descriptor.index)
        IrValueParameterImpl(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                newDescriptor,
                it.type,
                it.varargElementType
        )
    }


    constructorDescriptor.initialize(
            valueParameters.map { it.descriptor as ValueParameterDescriptor },
            superConstructorDescriptor.visibility
    )
    constructorDescriptor.returnType = superConstructorDescriptor.returnType

    return IrConstructorImpl(startOffset, endOffset, origin, constructorDescriptor, this.defaultType).also { constructor ->

        assert(superConstructor.dispatchReceiverParameter == null) // Inner classes aren't supported.

        constructor.valueParameters += valueParameters

        constructor.body = IrBlockBodyImpl(
                startOffset, endOffset,
                listOf(
                        IrDelegatingConstructorCallImpl(
                                startOffset, endOffset, irBuiltIns.unitType,
                                superConstructor.symbol, superConstructor.descriptor
                        ).apply {
                            constructor.valueParameters.forEachIndexed { idx, parameter ->
                                putValueArgument(idx, IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol))
                            }
                        },
                        IrInstanceInitializerCallImpl(startOffset, endOffset, this.symbol, irBuiltIns.unitType)
                )
        )

        constructor.parent = this
        this.declarations.add(constructor)
    }
}

val IrCall.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true
val IrFunctionReference.isSuspend get() = (symbol.owner as? IrSimpleFunction)?.isSuspend == true


fun IrValueParameter.copyTo(
    irFunction: IrFunction,
    shift: Int = 0,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrDeclarationOrigin = this.origin,
    name: Name = this.name,
    type: IrType = this.type.remapTypeParameters(this.parent as IrTypeParametersContainer, irFunction),
    varargElementType: IrType? = this.varargElementType
): IrValueParameter {
    val descriptor = WrappedValueParameterDescriptor(symbol.descriptor.annotations, symbol.descriptor.source)
    val symbol = IrValueParameterSymbolImpl(descriptor)
    val defaultValueCopy = defaultValue?.deepCopyWithVariables()
    defaultValueCopy?.patchDeclarationParents(irFunction)
    return IrValueParameterImpl(
        startOffset, endOffset, origin, symbol,
        name, shift + index, type, varargElementType, isCrossinline, isNoinline
    ).also {
        descriptor.bind(it)
        it.parent = irFunction
        it.defaultValue = defaultValueCopy
    }
}

fun IrTypeParameter.copyToWithoutSuperTypes(
    target: IrTypeParametersContainer,
    shift: Int = 0,
    origin: IrDeclarationOrigin = this.origin
): IrTypeParameter {
    val source = parent as IrTypeParametersContainer
    val descriptor = WrappedTypeParameterDescriptor(symbol.descriptor.annotations, symbol.descriptor.source)
    val symbol = IrTypeParameterSymbolImpl(descriptor)
    return IrTypeParameterImpl(startOffset, endOffset, origin, symbol, name, shift + index, isReified, variance).also { copied ->
        descriptor.bind(copied)
        copied.parent = target
    }
}

fun IrFunction.copyParameterDeclarationsFrom(from: IrFunction) {
    assert(typeParameters.isEmpty())
    copyTypeParametersFrom(from)

    // TODO: should dispatch receiver be copied?
    dispatchReceiverParameter = from.dispatchReceiverParameter?.let {
        IrValueParameterImpl(it.startOffset, it.endOffset, it.origin, it.descriptor, it.type, it.varargElementType).also {
            it.parent = this
        }
    }
    extensionReceiverParameter = from.extensionReceiverParameter?.copyTo(this)

    val shift = valueParameters.size
    valueParameters += from.valueParameters.map { it.copyTo(this, shift) }
}

fun IrTypeParametersContainer.copyTypeParametersFrom(
    source: IrTypeParametersContainer,
    origin: IrDeclarationOrigin? = null
) {
    val target = this
    val shift = target.typeParameters.size
    // Any type parameter can figure in a boundary type for any other parameter.
    // Therefore, we first copy the parameters themselves, then set up their supertypes.
    source.typeParameters.forEachIndexed { i, sourceParameter ->
        assert(sourceParameter.index == i)
        target.typeParameters.add(sourceParameter.copyToWithoutSuperTypes(target, shift = shift, origin = origin ?: sourceParameter.origin))
    }
    source.typeParameters.zip(target.typeParameters.drop(shift)).forEach { (srcParameter, dstParameter) ->
        dstParameter.copySuperTypesFrom(srcParameter)
    }
}

private fun IrTypeParameter.copySuperTypesFrom(source: IrTypeParameter) {
    val target = this
    val sourceParent = source.parent as IrTypeParametersContainer
    val targetParent = target.parent as IrTypeParametersContainer
    val shift = target.index - source.index
    source.superTypes.forEach {
        target.superTypes.add(it.remapTypeParameters(sourceParent, targetParent, shift))
    }
}

fun IrFunction.copyValueParametersToStatic(
    source: IrFunction,
    origin: IrDeclarationOrigin
) {
    val target = this
    assert(target.valueParameters.isEmpty())

    var shift = 0
    source.dispatchReceiverParameter?.apply {
        target.valueParameters.add(
            copyTo(
                target,
                origin = origin,
                shift = shift++,
                name = Name.identifier("\$this")
            )
        )
    }
    source.extensionReceiverParameter?.apply {
        target.valueParameters.add(
            copyTo(
                target,
                origin = origin,
                shift = shift++,
                name = Name.identifier("\$receiver")
            )
        )
    }
    source.valueParameters.forEachIndexed { i, oldValueParameter ->
        target.valueParameters.add(
            oldValueParameter.copyTo(
                target,
                origin = origin,
                shift = shift
            )
        )
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