/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_CLASS_ID

/**
 * A generator for delegated members from implementation by delegation.
 *
 * It assumes a synthetic field with the super-interface type has been created for the delegate expression. It looks for delegatable
 * methods and properties in the super-interface, and creates corresponding members in the subclass.
 * TODO: generic super interface types and generic delegated members.
 */
class DelegatedMemberGenerator(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    companion object {
        private val PLATFORM_DEPENDENT_CLASS_ID = ClassId.topLevel(FqName("kotlin.internal.PlatformDependent"))
    }

    private val baseFunctionSymbols = mutableMapOf<IrFunction, List<FirNamedFunctionSymbol>>()
    private val basePropertySymbols = mutableMapOf<IrProperty, List<FirPropertySymbol>>()

    // Generate delegated members for [subClass]. The synthetic field [irField] has the super interface type.
    fun generate(irField: IrField, firField: FirField, firSubClass: FirClass, subClass: IrClass) {
        val subClassLookupTag = firSubClass.symbol.toLookupTag()

        val subClassScope = firSubClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        subClassScope.processAllFunctions { functionSymbol ->
            val unwrapped =
                functionSymbol
                    .unwrapDelegateTarget(subClassLookupTag, firField)
                    ?: return@processAllFunctions

            val member =
                declarationStorage.getIrFunctionSymbol(unwrapped.symbol).owner as? IrSimpleFunction
                    ?: return@processAllFunctions

            if (shouldSkipDelegationFor(unwrapped)) {
                return@processAllFunctions
            }

            val irSubFunction = generateDelegatedFunction(
                subClass, firSubClass, irField, member, functionSymbol.fir
            )

            declarationStorage.cacheDelegationFunction(functionSymbol.fir, irSubFunction)
            subClass.addMember(irSubFunction)
        }

        subClassScope.processAllProperties { propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) return@processAllProperties

            val unwrapped =
                propertySymbol
                    .unwrapDelegateTarget(subClassLookupTag, firField)
                    ?: return@processAllProperties

            val member = declarationStorage.getIrPropertySymbol(unwrapped.symbol).owner as? IrProperty
                ?: return@processAllProperties

            if (shouldSkipDelegationFor(unwrapped)) {
                return@processAllProperties
            }

            val irSubProperty = generateDelegatedProperty(
                subClass, firSubClass, irField, member, propertySymbol.fir
            )

            declarationStorage.cacheDelegatedProperty(propertySymbol.fir, irSubProperty)
            subClass.addMember(irSubProperty)
        }
    }

    private fun shouldSkipDelegationFor(unwrapped: FirCallableDeclaration): Boolean {
        // See org.jetbrains.kotlin.resolve.jvm.JvmDelegationFilter
        return (unwrapped is FirSimpleFunction && unwrapped.isDefaultJavaMethod()) ||
                unwrapped.hasAnnotation(JVM_DEFAULT_CLASS_ID) ||
                unwrapped.hasAnnotation(PLATFORM_DEPENDENT_CLASS_ID)
    }

    private fun FirSimpleFunction.isDefaultJavaMethod(): Boolean =
        when {
            isIntersectionOverride ->
                baseForIntersectionOverride!!.isDefaultJavaMethod()
            isSubstitutionOverride ->
                originalForSubstitutionOverride!!.isDefaultJavaMethod()
            else -> {
                // Check that we have a non-abstract method from Java interface
                origin == FirDeclarationOrigin.Enhancement && modality == Modality.OPEN
            }
        }


    fun bindDelegatedMembersOverriddenSymbols(irClass: IrClass) {
        val superClasses by lazy(LazyThreadSafetyMode.NONE) {
            irClass.superTypes.mapNotNullTo(mutableSetOf()) { it.classifierOrNull?.owner as? IrClass }
        }
        for (declaration in irClass.declarations) {
            if (declaration.origin != IrDeclarationOrigin.DELEGATED_MEMBER) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    declaration.overriddenSymbols = baseFunctionSymbols[declaration]?.flatMap {
                        fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)
                    }?.filter { it.owner != declaration }.orEmpty()
                }
                is IrProperty -> {
                    declaration.overriddenSymbols = basePropertySymbols[declaration]?.flatMap {
                        fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)
                    }?.filter { it.owner != declaration }.orEmpty()
                }
                else -> continue
            }
        }
    }

    private fun generateDelegatedFunction(
        subClass: IrClass,
        firSubClass: FirClass,
        irField: IrField,
        superFunction: IrSimpleFunction,
        delegateOverride: FirSimpleFunction
    ): IrSimpleFunction {
        val delegateFunction =
            declarationStorage.createIrFunction(
                delegateOverride, subClass, predefinedOrigin = IrDeclarationOrigin.DELEGATED_MEMBER,
                containingClass = firSubClass.symbol.toLookupTag()
            )
        val baseSymbols = mutableListOf<FirNamedFunctionSymbol>()
        // the overridden symbols should be collected only after all fake overrides for all superclases are created and bound to their
        // overridden symbols, otherwise in some cases they will be left in inconsistent state leading to the errors in IR
        delegateOverride.processOverriddenFunctionSymbols(firSubClass, session, scopeSession) {
            baseSymbols.add(it)
        }
        baseFunctionSymbols[delegateFunction] = baseSymbols
        annotationGenerator.generate(delegateFunction, delegateOverride)

        val body = createDelegateBody(irField, delegateFunction, superFunction)
        delegateFunction.body = body
        return delegateFunction
    }

    private fun createDelegateBody(
        irField: IrField,
        delegateFunction: IrSimpleFunction,
        superFunction: IrSimpleFunction
    ): IrBlockBody {
        val startOffset = irField.startOffset
        val endOffset = irField.endOffset
        val body = irFactory.createBlockBody(startOffset, endOffset)
        val irCall = IrCallImpl(
            startOffset,
            endOffset,
            delegateFunction.returnType,
            superFunction.symbol,
            superFunction.typeParameters.size,
            superFunction.valueParameters.size
        ).apply {
            dispatchReceiver =
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irField.symbol,
                    irField.type,
                    IrGetValueImpl(
                        startOffset, endOffset,
                        delegateFunction.dispatchReceiverParameter?.type!!,
                        delegateFunction.dispatchReceiverParameter?.symbol!!
                    )
                )
            extensionReceiver =
                delegateFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.type, extensionReceiver.symbol)
                }
            delegateFunction.valueParameters.forEach {
                putValueArgument(it.index, IrGetValueImpl(startOffset, endOffset, it.type, it.symbol))
            }
        }
        if (superFunction.returnType.isUnit() || superFunction.returnType.isNothing()) {
            body.statements.add(irCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, delegateFunction.symbol, irCall)
            body.statements.add(irReturn)
        }
        return body
    }

    private fun generateDelegatedProperty(
        subClass: IrClass,
        firSubClass: FirClass,
        irField: IrField,
        superProperty: IrProperty,
        firDelegateProperty: FirProperty
    ): IrProperty {
        val delegateProperty =
            declarationStorage.createIrProperty(
                firDelegateProperty, subClass, predefinedOrigin = IrDeclarationOrigin.DELEGATED_MEMBER,
                containingClass = firSubClass.symbol.toLookupTag()
            )
        // the overridden symbols should be collected only after all fake overrides for all superclases are created and bound to their
        // overridden symbols, otherwise in some cases they will be left in inconsistent state leading to the errors in IR
        val baseSymbols = mutableListOf<FirPropertySymbol>()
        firDelegateProperty.processOverriddenPropertySymbols(firSubClass, session, scopeSession) {
            baseSymbols.add(it)
        }
        basePropertySymbols[delegateProperty] = baseSymbols
        annotationGenerator.generate(delegateProperty, firDelegateProperty)

        delegateProperty.getter!!.body = createDelegateBody(irField, delegateProperty.getter!!, superProperty.getter!!)
        delegateProperty.getter!!.overriddenSymbols =
            firDelegateProperty.generateOverriddenAccessorSymbols(
                firSubClass,
                isGetter = true,
                session,
                scopeSession,
                declarationStorage,
                fakeOverrideGenerator
            )
        annotationGenerator.generate(delegateProperty.getter!!, firDelegateProperty)
        if (delegateProperty.isVar) {
            delegateProperty.setter!!.body = createDelegateBody(irField, delegateProperty.setter!!, superProperty.setter!!)
            delegateProperty.setter!!.overriddenSymbols =
                firDelegateProperty.generateOverriddenAccessorSymbols(
                    firSubClass, isGetter = false, session, scopeSession, declarationStorage, fakeOverrideGenerator
                )
            annotationGenerator.generate(delegateProperty.setter!!, firDelegateProperty)
        }

        return delegateProperty
    }

}

private fun <S : FirCallableSymbol<D>, D : FirCallableDeclaration> S.unwrapDelegateTarget(
    subClassLookupTag: ConeClassLikeLookupTag,
    firField: FirField,
): D? {
    val callable = this.fir as? D ?: return null

    val delegatedWrapperData = callable.delegatedWrapperData ?: return null
    if (delegatedWrapperData.containingClass != subClassLookupTag) return null
    if (delegatedWrapperData.delegateField != firField) return null

    val wrapped = delegatedWrapperData.wrapped as? D ?: return null

    @Suppress("UNCHECKED_CAST")
    val wrappedSymbol = wrapped.symbol as? S ?: return null

    @Suppress("UNCHECKED_CAST")
    return wrappedSymbol.unwrapCallRepresentative().fir as D
}
