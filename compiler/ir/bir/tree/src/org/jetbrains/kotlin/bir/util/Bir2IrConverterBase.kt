/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrCapturedType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.Name
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class Bir2IrConverterBase(
    private val remapedIr2BirElements: Map<BirElement, IrSymbol>,
    private val compiledBir: BirForest,
) {
    var elementConvertedCallbacck: ((BirElement, IrElement) -> Unit)? = null

    protected fun <Ir : IrElement, Bir : BirElement> createElementMap(expectedMaxSize: Int = 8): MutableMap<Bir, Ir> =
        IdentityHashMap<Bir, Ir>(expectedMaxSize)

    protected abstract fun <Ir : IrElement> copyElement(old: BirElement): Ir


    protected fun <Ir : IrElement> IrElement.copyChildElement(old: BirElement): Ir = copyElement<Ir>(old)

    protected fun <Ir : IrElement> copyChildElement(old: BirElement): Ir = copyElement(old)

    @JvmName("copyChildElementNullable")
    protected inline fun <Ir : IrElement, reified Bir : BirElement?> copyChildElement(old: Bir): Ir {
        return if (old != null) {
            copyElement(old)
        } else {
            DummyIrElements.createForClass(Bir::class.java) as Ir
        }
    }


    fun <Ir : IrElement> remapElement(old: BirElement): Ir = copyElement(old)

    @JvmName("remapElementNullable")
    fun <Ir : IrElement> remapElement(old: BirElement?): Ir? = if (old == null) null else copyElement(old)

    protected inline fun <Bir : BirElement, ME : IrElement, SE : ME> copyNotReferencedElement(
        old: Bir,
        copy: () -> SE,
        lateInitialize: SE.() -> Unit,
    ): SE {
        val new = copy()
        lateInitialize(new)
        elementConvertedCallbacck?.invoke(old, new)
        return new
    }

    protected fun <Bir : BirElement, ME : IrElement, SE : ME> copyReferencedElement(
        old: Bir,
        map: MutableMap<Bir, ME>,
        copy: () -> SE,
        lateInitialize: SE.() -> Unit,
    ): SE {
        map[old]?.let {
            @Suppress("UNCHECKED_CAST")
            return it as SE
        }

        @Suppress("UNCHECKED_CAST")
        val new = remapedIr2BirElements[old]?.owner as SE?
            ?: copy()
        map[old] = new
        lateInitialize(new)
        elementConvertedCallbacck?.invoke(old, new)
        return new
    }

    fun <IrS : IrSymbol, BirS : BirSymbol> remapSymbol(old: BirS): IrS {
        val birElement = old.owner
        if (birElement is BirLazyElementBase) {
            @Suppress("UNCHECKED_CAST")
            return birElement.originalIrElement.symbol as IrS
        }

        val irElement = remapElement<IrSymbolOwner>(birElement)
        @Suppress("UNCHECKED_CAST")
        return irElement.symbol as IrS
    }

    @JvmName("remapSymbolNullable")
    fun <IrS : IrSymbol, BirS : BirSymbol> remapSymbol(old: BirS?): IrS? = if (old == null) null else remapSymbol(old)

    protected fun <BirS : BirSymbol, IrS : IrBindableSymbol<*, *>> createBindableSymbol(old: BirS): IrS {
        val descriptor = (old as? BirSymbolOwner)?.descriptor

        @Suppress("UNCHECKED_CAST")
        return when (old) {
            is BirFileSymbol -> IrFileSymbolImpl(descriptor as PackageFragmentDescriptor?)
            is BirExternalPackageFragmentSymbol -> IrExternalPackageFragmentSymbolImpl(descriptor as PackageFragmentDescriptor?)
            is BirAnonymousInitializerSymbol -> IrAnonymousInitializerSymbolImpl(descriptor as ClassDescriptor?)
            is BirEnumEntrySymbol -> IrEnumEntrySymbolImpl(descriptor as ClassDescriptor?)
            is BirFieldSymbol -> IrFieldSymbolImpl(descriptor as PropertyDescriptor?)
            is BirClassSymbol -> IrClassSymbolImpl(descriptor as ClassDescriptor?)
            is BirScriptSymbol -> IrScriptSymbolImpl(descriptor as ScriptDescriptor?)
            is BirTypeParameterSymbol -> IrTypeParameterSymbolImpl(descriptor as TypeParameterDescriptor?)
            is BirValueParameterSymbol -> IrValueParameterSymbolImpl(descriptor as ParameterDescriptor?)
            is BirVariableSymbol -> IrVariableSymbolImpl(descriptor as VariableDescriptor?)
            is BirConstructorSymbol -> IrConstructorSymbolImpl(descriptor as ClassConstructorDescriptor?)
            is BirSimpleFunctionSymbol -> IrSimpleFunctionSymbolImpl(descriptor as FunctionDescriptor?)
            is BirReturnableBlockSymbol -> IrReturnableBlockSymbolImpl(descriptor as FunctionDescriptor?)
            is BirPropertySymbol -> IrPropertySymbolImpl(descriptor as PropertyDescriptor?)
            is BirLocalDelegatedPropertySymbol -> IrLocalDelegatedPropertySymbolImpl(descriptor as VariableDescriptorWithAccessors?)
            is BirTypeAliasSymbol -> IrTypeAliasSymbolImpl(descriptor as TypeAliasDescriptor?)
            else -> error(old)
        } as IrS
    }

    protected fun IrAttributeContainer.copyAttributes(old: BirAttributeContainer) {
        val owner = old.attributeOwnerId
        attributeOwnerId = if (owner === old) this else remapElement(owner)
    }

    protected fun IrMemberAccessExpression<*>.copyIrMemberAccessExpressionArguments(from: BirMemberAccessExpression<*>) {
        for (i in 0 until from.valueArguments.size) {
            val arg = from.valueArguments[i]
            putValueArgument(i, remapElement(arg))
        }
        for (i in from.typeArguments.indices) {
            val arg = from.typeArguments[i]
            putTypeArgument(i, remapType(arg))
        }
    }

    protected val IrMemberAccessExpression<*>.typeArguments: List<IrType?>
        get() = List(typeArgumentsCount) { getTypeArgument(it) }

    protected inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.copyTo(destination: C, transform: (T) -> R): C {
        destination.clear()
        for (item in this)
            destination.add(transform(item))
        return destination
    }


    fun remapType(birType: BirType): IrType = when (birType) {
        is BirSimpleType -> remapSimpleType(birType)
        is BirCapturedType -> remapCapturedType(birType)
        is BirDynamicType -> remapDynamicType(birType)
        is BirErrorType -> remapErrorType(birType)
        else -> TODO(birType.toString())
    }

    @JvmName("remapTypeNullable")
    fun remapType(birType: BirType?): IrType? = if (birType == null) null else remapType(birType)

    fun remapSimpleType(birType: BirSimpleType): IrSimpleType {
        return IrSimpleTypeImpl(
            birType.kotlinType,
            remapSymbol(birType.classifier),
            birType.nullability,
            birType.arguments.map { remapTypeArgument(it) },
            birType.annotations.map { remapElement(it) as IrConstructorCall },
            birType.abbreviation?.let { abbreviation ->
                remapTypeAbbreviation(abbreviation)
            },
        )
    }

    private fun remapTypeAbbreviation(abbreviation: BirTypeAbbreviation): IrTypeAbbreviation {
        return IrTypeAbbreviationImpl(
            remapSymbol(abbreviation.typeAlias),
            abbreviation.hasQuestionMark,
            abbreviation.arguments.map { remapTypeArgument(it) },
            abbreviation.annotations.map { remapElement(it) as IrConstructorCall },
        )
    }

    private fun remapCapturedType(birType: BirCapturedType): IrCapturedType {
        return IrCapturedType(
            birType.captureStatus,
            birType.lowerType?.let { remapType(it) },
            remapTypeArgument(birType.constructor.argument),
            remapElement(birType.constructor.typeParameter) as IrTypeParameter,
        )
    }

    private fun remapDynamicType(birType: BirDynamicType): IrDynamicType {
        return IrDynamicTypeImpl(
            birType.kotlinType,
            birType.annotations.map { remapElement(it) as IrConstructorCall },
            birType.variance,
        )
    }

    private fun remapErrorType(birType: BirErrorType): IrErrorType {
        return IrErrorTypeImpl(
            birType.kotlinType,
            birType.annotations.map { remapElement(it) as IrConstructorCall },
            birType.variance,
            birType.isMarkedNullable,
        )
    }

    fun remapTypeArgument(birTypeArgument: BirTypeArgument): IrTypeArgument = when (birTypeArgument) {
        is BirStarProjection -> IrStarProjectionImpl
        is BirType -> remapType(birTypeArgument) as IrTypeArgument
        is BirTypeProjectionImpl -> makeTypeProjection(remapType(birTypeArgument.type), birTypeArgument.variance)
        else -> error(birTypeArgument)
    }

    companion object {
        fun IrElement.convertToBir(): BirElement {
            val converter = Ir2BirConverter(BirElementDynamicPropertyManager())
            return converter.copyIrTree(listOf(this)).single()
        }
    }

    /*private object PartialPatchParentVisitor : IrElementVisitor<IrElement, BirElementParent> {
        override fun visitElement(element: IrElement, data: BirElementParent): IrElement {
            if (element is IrDeclaration) {
                if(element.parent)
            }
        }
    }*/

    protected object DummyIrElements {
        fun createForClass(birClass: Class<*>): IrElement {
            return when (birClass) {
                BirSimpleFunction::class.java -> DummyIrElements.irSimpleFunction
                else -> TODO(birClass.simpleName)
            }
        }

        val irSimpleFunction = IrFunctionImpl(
            0, 0, IrDeclarationOrigin.DEFINED, IrSimpleFunctionSymbolImpl(), Name.identifier(""), DescriptorVisibilities.PUBLIC,
            Modality.FINAL, IrUninitializedType, false, false, false, false, false, false, false, false
        )
    }
}