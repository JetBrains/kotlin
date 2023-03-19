/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.filterUseSiteTarget
import org.jetbrains.kotlin.fir.builder.initContainingClassAttr
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.copyWithNewSourceKind
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isFromVararg
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.references.builder.buildPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class ValueParameter(
    private val isVal: Boolean,
    private val isVar: Boolean,
    private val modifiers: Modifier,
    val returnTypeRef: FirTypeRef,
    val source: KtSourceElement,
    private val moduleData: FirModuleData,
    private val isFromPrimaryConstructor: Boolean,
    private val additionalAnnotations: List<FirAnnotation>,
    val name: Name,
    val defaultValue: FirExpression?,
    private val containingFunctionSymbol: FirFunctionSymbol<*>?,
    val destructuringDeclaration: DestructuringDeclaration? = null
) {
    fun hasValOrVar(): Boolean {
        return isVal || isVar
    }

    val annotations: List<FirAnnotation> by lazy(LazyThreadSafetyMode.NONE) {
        buildList {
            if (!isFromPrimaryConstructor)
                addAll(modifiers.annotations)
            else
                modifiers.annotations.filterTo(this) {
                    val useSiteTarget = it.useSiteTarget
                    useSiteTarget == null || useSiteTarget == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER || useSiteTarget == AnnotationUseSiteTarget.RECEIVER || useSiteTarget == AnnotationUseSiteTarget.FILE
                }
            addAll(additionalAnnotations)
        }
    }

    val firValueParameter: FirValueParameter by lazy(LazyThreadSafetyMode.NONE) {
        buildValueParameter {
            source = this@ValueParameter.source
            moduleData = this@ValueParameter.moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = this@ValueParameter.returnTypeRef
            this.name = this@ValueParameter.name
            symbol = FirValueParameterSymbol(name)
            defaultValue = this@ValueParameter.defaultValue
            isCrossinline = modifiers.hasCrossinline()
            isNoinline = modifiers.hasNoinline()
            isVararg = modifiers.hasVararg()
            containingFunctionSymbol = this@ValueParameter.containingFunctionSymbol
                ?: error("containingFunctionSymbol should present when converting ValueParameter to a FirValueParameter")

            annotations += this@ValueParameter.annotations
            annotations += additionalAnnotations
        }
    }

    fun <T> toFirPropertyFromPrimaryConstructor(
        moduleData: FirModuleData,
        callableId: CallableId,
        isExpect: Boolean,
        currentDispatchReceiver: ConeClassLikeType?,
        context: Context<T>
    ): FirProperty {
        val name = this.firValueParameter.name
        var type = this.firValueParameter.returnTypeRef
        if (type is FirImplicitTypeRef) {
            type = buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Incomplete code", DiagnosticKind.Syntax) }
        }

        return buildProperty {
            val propertySource = firValueParameter.source?.fakeElement(KtFakeSourceElementKind.PropertyFromParameter)
            source = propertySource
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = type.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyFromParameter)
            this.name = name
            initializer = buildPropertyAccessExpression {
                source = propertySource
                calleeReference = buildPropertyFromParameterResolvedNamedReference {
                    source = propertySource
                    this.name = name
                    resolvedSymbol = this@ValueParameter.firValueParameter.symbol
                    source = propertySource
                }
            }
            isVar = this@ValueParameter.isVar
            symbol = FirPropertySymbol(callableId)
            dispatchReceiverType = currentDispatchReceiver
            isLocal = false
            status = FirDeclarationStatusImpl(modifiers.getVisibility(), modifiers.getModality(isClassOrObject = false)).apply {
                this.isExpect = isExpect
                isActual = modifiers.hasActual()
                isOverride = modifiers.hasOverride()
                isConst = modifiers.hasConst()
            }
            backingField = FirDefaultPropertyBackingField(
                moduleData = moduleData,
                annotations = mutableListOf(),
                returnTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                isVar = isVar,
                propertySymbol = symbol,
                status = status.copy(),
            )
            annotations += modifiers.annotations.filter {
                it.useSiteTarget == null || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY ||
                        it.useSiteTarget == AnnotationUseSiteTarget.FIELD ||
                        it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            }
            val defaultAccessorSource = propertySource?.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
            getter = FirDefaultPropertyGetter(
                defaultAccessorSource,
                moduleData,
                FirDeclarationOrigin.Source,
                type.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                modifiers.getVisibility(),
                symbol,
            ).also {
                it.initContainingClassAttr(context)
                it.replaceAnnotations(modifiers.annotations.filterUseSiteTarget(AnnotationUseSiteTarget.PROPERTY_GETTER))
            }
            setter = if (this.isVar) FirDefaultPropertySetter(
                defaultAccessorSource,
                moduleData,
                FirDeclarationOrigin.Source,
                type.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                modifiers.getVisibility(),
                symbol,
                parameterAnnotations = modifiers.annotations.filterUseSiteTarget(AnnotationUseSiteTarget.SETTER_PARAMETER)
            ).also {
                it.initContainingClassAttr(context)
                it.replaceAnnotations(modifiers.annotations.filterUseSiteTarget(AnnotationUseSiteTarget.PROPERTY_SETTER))
            } else null
        }.apply {
            if (firValueParameter.isVararg) {
                this.isFromVararg = true
            }
            firValueParameter.correspondingProperty = this
            this.fromPrimaryConstructor = true
        }
    }
}
