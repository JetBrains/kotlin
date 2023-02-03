/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.wrapIntoFileAnalysisExceptionIfNeeded
import org.jetbrains.kotlin.util.wrapIntoSourceCodeAnalysisExceptionIfNeeded

// TODO: rewrite
fun FirBlock.returnExpressions(): List<FirExpression> = listOfNotNull(statements.lastOrNull() as? FirExpression)

fun <R : FirTypeRef> R.copyWithNewSourceKind(newKind: KtFakeSourceElementKind): R {
    if (source == null) return this
    if (source?.kind == newKind) return this
    return copyWithNewSource(source?.fakeElement(newKind))
}

// do we need a deep copy here ?
fun <R : FirTypeRef> R.copyWithNewSource(newSource: KtSourceElement?): R {
    if (source?.kind == newSource?.kind) return this

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
        is FirImplicitBuiltinTypeRef -> typeRef.withNewSource(newSource)
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
    hasStableParameterNames: Boolean = this.hasStableParameterNames,
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
        this.hasStableParameterNames = hasStableParameterNames
    }
}

inline fun <R> whileAnalysing(session: FirSession, element: FirElement, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        session.exceptionHandler.handleExceptionOnElementAnalysis(element, throwable)
    }
}

inline fun <R> withFileAnalysisExceptionWrapping(file: FirFile, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        file.moduleData.session.exceptionHandler.handleExceptionOnFileAnalysis(file, throwable)
    }
}

abstract class FirExceptionHandler : FirSessionComponent {
    abstract fun handleExceptionOnElementAnalysis(element: FirElement, throwable: Throwable): Nothing
    abstract fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing
}

val FirSession.exceptionHandler: FirExceptionHandler by FirSession.sessionComponentAccessor()

object FirCliExceptionHandler : FirExceptionHandler() {
    override fun handleExceptionOnElementAnalysis(element: FirElement, throwable: Throwable): Nothing {
        throw throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element.source)
    }

    override fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing {
        throw throwable.wrapIntoFileAnalysisExceptionIfNeeded(
            file.sourceFile?.path,
            file.source
        ) { file.sourceFileLinesMapping?.getLineAndColumnByOffset(it) }
    }
}

@JvmInline
value class MutableOrEmptyList<out T>(internal val list: MutableList<@UnsafeVariance T>?) : List<T> {

    private constructor(list: Nothing?) : this(list as MutableList<T>?)

    override val size: Int
        get() = list?.size ?: 0

    override fun get(index: Int): T {
        return list!![index]
    }

    override fun isEmpty(): Boolean {
        return list?.isEmpty() ?: true
    }

    override fun iterator(): Iterator<T> {
        return list?.iterator() ?: EMPTY_LIST_STUB_ITERATOR
    }

    override fun listIterator(): ListIterator<T> {
        return list?.listIterator() ?: EMPTY_LIST_STUB_LIST_ITERATOR
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return list?.listIterator(index) ?: EMPTY_LIST_STUB_LIST_ITERATOR
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        if (list == null && fromIndex == 0 && toIndex == 0) return this
        return list!!.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: @UnsafeVariance T): Int {
        return list?.lastIndexOf(element) ?: -1
    }

    override fun indexOf(element: @UnsafeVariance T): Int {
        return list?.indexOf(element) ?: -1
    }

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return list?.containsAll(elements) ?: elements.isEmpty()
    }

    override fun contains(element: @UnsafeVariance T): Boolean {
        return list?.contains(element) ?: false
    }

    override fun toString(): String {
        return list?.joinToString(prefix = "[", postfix = "]") ?: "[]"
    }

    companion object {
        private val EMPTY = MutableOrEmptyList<Nothing>(null)

        private val EMPTY_LIST_STUB = emptyList<Nothing>()

        private val EMPTY_LIST_STUB_ITERATOR = EMPTY_LIST_STUB.iterator()

        private val EMPTY_LIST_STUB_LIST_ITERATOR = EMPTY_LIST_STUB.listIterator()

        fun <T> empty(): MutableOrEmptyList<T> = EMPTY
    }
}

fun <T> List<T>.smartPlus(other: List<T>): List<T> = when {
    other.isEmpty() -> this
    this.isEmpty() -> other
    else -> {
        val result = ArrayList<T>(this.size + other.size)
        result.addAll(this)
        result.addAll(other)
        result
    }
}
