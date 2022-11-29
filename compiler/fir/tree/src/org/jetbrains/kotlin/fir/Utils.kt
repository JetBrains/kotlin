/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// TODO: rewrite
fun FirBlock.returnExpressions(): List<FirExpression> = listOfNotNull(statements.lastOrNull() as? FirExpression)

// do we need a deep copy here ?
fun <R : FirTypeRef> R.copyWithNewSourceKind(newKind: KtFakeSourceElementKind): R {
    if (source == null) return this
    if (source?.kind == newKind) return this
    val newSource = source?.fakeElement(newKind)

    @Suppress("UNCHECKED_CAST")
    return when (val typeRef = this) {
        is FirResolvedTypeRefImpl -> buildResolvedTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirErrorTypeRef -> buildErrorTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirUserTypeRefImpl -> buildUserTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            qualifier += typeRef.qualifier
            annotations += typeRef.annotations
        }
        is FirImplicitTypeRef -> buildImplicitTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirFunctionTypeRefImpl -> buildFunctionTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirDynamicTypeRef -> buildDynamicTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            annotations += typeRef.annotations
        }
        is FirImplicitBuiltinTypeRef -> typeRef.withFakeSource(newKind)
        is FirIntersectionTypeRef -> buildIntersectionTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            leftType = typeRef.leftType
            rightType = typeRef.rightType
        }
        else -> TODO("Not implemented for ${typeRef::class}")
    } as R
}

val FirFile.packageFqName: FqName
    get() = packageDirective.packageFqName

val FirElement.psi: PsiElement? get() = (source as? KtPsiSourceElement)?.psi
val FirElement.realPsi: PsiElement? get() = (source as? KtRealPsiSourceElement)?.psi

val FirReference.resolved: FirResolvedNamedReference? get() = this as? FirResolvedNamedReference
val FirReference.resolvedSymbol: FirBasedSymbol<*>? get() = resolved?.resolvedSymbol

val FirContextReceiver.labelName: Name? get() = customLabelName ?: labelNameFromTypeRef

fun FirElement.renderWithType(): String =
    FirRenderer().renderElementWithTypeAsString(this)

fun FirElement.render(): String =
    FirRenderer().renderElementAsString(this)

fun FirDeclarationStatus.copy(
    visibility: Visibility? = this.visibility,
    modality: Modality? = this.modality,
    isExpect: Boolean = this.isExpect,
    isActual: Boolean = this.isActual,
    isOverride: Boolean = this.isOverride,
    isOperator: Boolean = this.isOperator,
    isInfix: Boolean = this.isInfix,
    isInline: Boolean = this.isInline,
    isTailRec: Boolean = this.isTailRec,
    isExternal: Boolean = this.isExternal,
    isConst: Boolean = this.isConst,
    isLateInit: Boolean = this.isLateInit,
    isInner: Boolean = this.isInner,
    isCompanion: Boolean = this.isCompanion,
    isData: Boolean = this.isData,
    isSuspend: Boolean = this.isSuspend,
    isStatic: Boolean = this.isStatic,
    isFromSealedClass: Boolean = this.isFromSealedClass,
    isFromEnumClass: Boolean = this.isFromEnumClass,
    isFun: Boolean = this.isFun,
): FirDeclarationStatus {
    val newVisibility = visibility ?: this.visibility
    val newModality = modality ?: this.modality
    val newStatus = if (this is FirResolvedDeclarationStatus) {
        FirResolvedDeclarationStatusImpl(newVisibility, newModality!!, effectiveVisibility)
    } else {
        FirDeclarationStatusImpl(newVisibility, newModality)
    }
    return newStatus.apply {
        this.isExpect = isExpect
        this.isActual = isActual
        this.isOverride = isOverride
        this.isOperator = isOperator
        this.isInfix = isInfix
        this.isInline = isInline
        this.isTailRec = isTailRec
        this.isExternal = isExternal
        this.isConst = isConst
        this.isLateInit = isLateInit
        this.isInner = isInner
        this.isCompanion = isCompanion
        this.isData = isData
        this.isSuspend = isSuspend
        this.isStatic = isStatic
        this.isFromSealedClass = isFromSealedClass
        this.isFromEnumClass = isFromEnumClass
        this.isFun = isFun
    }
}
