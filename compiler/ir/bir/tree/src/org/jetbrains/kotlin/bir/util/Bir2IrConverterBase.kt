/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(IrImplementationDetail::class)

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class Bir2IrConverterBase(
    dynamicPropertyManager: BirDynamicPropertiesManager,
    override val compressedSourceSpanManager: CompressedSourceSpanManager,
    protected val remappedIr2BirElements: Map<BirElement, IrElement>,
    protected val compiledBir: BirDatabase,
) : CompressedSourceSpanManagerScope {
    var elementConvertedCallback: ((BirElement, IrElement) -> Unit)? = null
    var reuseOnlyExternalElements = false
    var remappedIr2BirTypes: Map<BirType, IrType>? = null

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
            @Suppress("UNCHECKED_CAST")
            DummyIrElements.createForClass(Bir::class.java) as Ir
        }
    }


    fun <Ir : IrElement> remapElement(old: BirElement): Ir = copyElement(old)

    @JvmName("remapElementNullable")
    fun <Ir : IrElement> remapElement(old: BirElement?): Ir? = if (old == null) null else copyElement(old)

    protected inline fun <Bir : BirElement, ME : IrElement, reified SE : ME> copyNotReferencedElement(
        old: Bir,
        copy: () -> SE,
        lateInitialize: SE.() -> Unit,
    ): SE {
        (tryReuseExternalElement<Bir, ME>(old) as? SE)?.let {
            return it
        }

        val new = reuseOnlyExternalElements.ifFalse {
            remappedIr2BirElements[old] as? SE
        } ?: copy().also { setParent(it, old) }
        lateInitialize(new)
        elementConvertedCallback?.invoke(old, new)
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

        tryReuseExternalElement<Bir, ME>(old)?.let {
            @Suppress("UNCHECKED_CAST")
            return it as SE
        }

        @Suppress("UNCHECKED_CAST")
        val new = reuseOnlyExternalElements.ifFalse {
            remappedIr2BirElements[old] as SE?
        } ?: copy().also { setParent(it, old) }
        map[old] = new
        lateInitialize(new)
        elementConvertedCallback?.invoke(old, new)
        return new
    }

    protected fun <Bir : BirElement, ME : IrElement> tryReuseExternalElement(old: Bir): IrElement? {
        if (reuseOnlyExternalElements && (old as BirElementBase).getContainingDatabase().let { it != null && it !== compiledBir }) {
            (remappedIr2BirElements[old])?.let {
                return it
            }
        }
        return null
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
        var descriptor = (old as? BirModuleFragment)?.descriptor
            ?: (old as? BirDeclaration)?.get(GlobalBirDynamicProperties.Descriptor)

        // workaround for a crash happening due to some unimplemented logic
        if (descriptor is IrBasedDeclarationDescriptor<*>) {
            val parent = descriptor.owner.parent
            if (parent is IrSymbolOwner && parent !is IrDeclaration && parent.symbol is DescriptorlessExternalPackageFragmentSymbol) {
                descriptor = null
            }
        }

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

    protected fun setParent(new: IrElement, old: BirElement) {
        if (new is IrDeclaration) {
            old.ancestors().firstIsInstanceOrNull<BirDeclarationParent>()?.let {
                new.parent = remapElement(it)
            }
        }
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


    fun remapType(birType: BirType): IrType {
        remappedIr2BirTypes?.get(birType)?.let {
            return it
        }

        return when (birType) {
            is BirCapturedType -> remapCapturedType(birType)
            is BirSimpleType -> remapSimpleType(birType)
            is BirDynamicType -> remapDynamicType(birType)
            is BirErrorType -> remapErrorType(birType)
            else -> TODO(birType.toString())
        }
    }

    @JvmName("remapTypeNullable")
    fun remapType(birType: BirType?): IrType? = if (birType == null) null else remapType(birType)

    private fun remapSimpleType(birType: BirSimpleType): IrSimpleType {
        return IrSimpleTypeImpl(
            birType.kotlinType,
            remapSymbol(birType.classifier),
            birType.nullability,
            birType.arguments.map { remapTypeArgument(it) },
            birType.annotations.map { remapElement(it) as IrConstructorCall },
            birType.abbreviation?.let { remapTypeAbbreviation(it) },
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
            birType.nullability,
            birType.annotations.map { remapElement(it) as IrConstructorCall },
            birType.abbreviation?.let { remapTypeAbbreviation(it) },
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

    private fun remapTypeArgument(birTypeArgument: BirTypeArgument): IrTypeArgument = when (birTypeArgument) {
        is BirStarProjection -> IrStarProjectionImpl
        is BirType -> remapType(birTypeArgument) as IrTypeArgument
        is BirTypeProjectionImpl -> makeTypeProjection(remapType(birTypeArgument.type), birTypeArgument.variance)
        else -> error(birTypeArgument)
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
            startOffset = 0,
            endOffset = 0,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier(""),
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isExternal = false,
            containerSource = null,
            isInline = false,
            isExpect = false,
            isTailrec = false,
            isSuspend = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false,
            factory = IrFactoryImpl,
        )
    }
}