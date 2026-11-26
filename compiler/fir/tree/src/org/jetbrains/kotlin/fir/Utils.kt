/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildWhenSubjectExpression
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.packageName
import org.jetbrains.kotlin.resolve.ReturnValueStatus
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.STATEMENT_LIKE_OPERATORS
import org.jetbrains.kotlin.util.wrapIntoFileAnalysisExceptionIfNeeded
import org.jetbrains.kotlin.util.wrapIntoSourceCodeAnalysisExceptionIfNeeded
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

val FirBlock.lastExpression: FirExpression?
    get() = statements.lastOrNull() as? FirExpression

fun <R : FirTypeRef> R.copyWithNewSourceKind(newKind: KtFakeSourceElementKind): R {
    val source = source ?: return this
    if (source.kind == newKind) return this
    return copyWithNewSource(source.fakeElement(newKind))
}

// do we need a deep copy here ?
fun <R : FirTypeRef> R.copyWithNewSource(newSource: KtSourceElement): R {
    if (source?.kind == newSource.kind) return this
    // The function type kept as the partially resolved type ref of incomplete code like `context(A)` keeps its source kind
    // when the error type ref is copied: the kind tells the diagnostic collector not to report anything on it
    if (source?.kind == KtFakeSourceElementKind.IncompleteFunctionType) return this

    val result = when (val typeRef = this) {
        is FirResolvedTypeRefImpl -> buildResolvedTypeRefCopy(typeRef) {
            source = newSource
        }
        is FirErrorTypeRef -> buildErrorTypeRefCopy(typeRef) {
            source = newSource
            partiallyResolvedTypeRef = typeRef.partiallyResolvedTypeRef?.copyWithNewSource(newSource)
        }
        is FirUserTypeRefImpl -> buildUserTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            qualifier += typeRef.qualifier
            annotations += typeRef.annotations
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
        is FirUnionTypeRef -> buildUnionTypeRef {
            source = newSource
            isMarkedNullable = typeRef.isMarkedNullable
            annotations += typeRef.annotations
            types += typeRef.types
        }
        else -> TODO("Not implemented for ${typeRef::class}")
    }
    @Suppress("UNCHECKED_CAST")
    return result as R
}

val FirFile.packageFqName: FqName
    get() = packageDirective.packageFqName

val FirFileSymbol.packageFqName: FqName
    get() = fir.packageFqName

val FirElement.psi: PsiElement? get() = (source as? KtPsiSourceElement)?.psi
val FirElement.realPsi: PsiElement? get() = (source as? KtRealPsiSourceElement)?.psi

fun FirElement.renderWithType(): String =
    FirRenderer().renderElementWithTypeAsString(this)

fun FirElement.render(): String =
    FirRenderer().renderElementAsString(this)

fun FirCallableDeclarationStatus.copy(
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
    isSuspend: Boolean = this.isSuspend,
    isStatic: Boolean = this.isStatic,
    isFromSealedClass: Boolean = this.isFromSealedClass,
    isFromEnumClass: Boolean = this.isFromEnumClass,
    hasStableParameterNames: Boolean = this.hasStableParameterNames,
    returnValueStatus: ReturnValueStatus = this.returnValueStatus
): FirCallableDeclarationStatus {
    val newVisibility = visibility ?: this.visibility
    val newModality = modality ?: this.modality
    val newStatus = if (this is FirResolvedDeclarationStatus) {
        FirResolvedCallableDeclarationStatusImpl(newVisibility, newModality!!, effectiveVisibility)
    } else {
        FirCallableDeclarationStatusImpl(newVisibility, newModality)
    }
    copyStatusAttributes(
        from = this,
        to = newStatus,
        isExpect = isExpect,
        isActual = isActual,
        isOverride = isOverride,
        isOperator = isOperator,
        isInfix = isInfix,
        isInline = isInline,
        isTailRec = isTailRec,
        isExternal = isExternal,
        isConst = isConst,
        isLateInit = isLateInit,
        isSuspend = isSuspend,
        isStatic = isStatic,
        isFromSealedClass = isFromSealedClass,
        isFromEnumClass = isFromEnumClass,
        hasStableParameterNames = hasStableParameterNames,
        returnValueStatus = returnValueStatus,
    )
    return newStatus
}
fun FirClassLikeDeclarationStatus.copy(
    visibility: Visibility? = this.visibility,
    modality: Modality? = this.modality,
    isExpect: Boolean = this.isExpect,
    isActual: Boolean = this.isActual,
    isInline: Boolean = this.isInline,
    isValue: Boolean = this.isValue,
    isExternal: Boolean = this.isExternal,
    isInner: Boolean = this.isInner,
    isCompanion: Boolean = this.isCompanion,
    isData: Boolean = this.isData,
    isFromSealedClass: Boolean = this.isFromSealedClass,
    isFromEnumClass: Boolean = this.isFromEnumClass,
    isFun: Boolean = this.isFun,
    isRichError: Boolean = this.isRichError,
): FirClassLikeDeclarationStatus {
    val newVisibility = visibility ?: this.visibility
    val newModality = modality ?: this.modality
    val newStatus = if (this is FirResolvedDeclarationStatus) {
        FirResolvedClassLikeDeclarationStatusImpl(newVisibility, newModality!!, effectiveVisibility)
    } else {
        FirClassLikeDeclarationStatusImpl(newVisibility, newModality)
    }
    copyStatusAttributes(
        from = this,
        to = newStatus,
        isExpect = isExpect,
        isActual = isActual,
        isInline = isInline,
        isValue = isValue,
        isExternal = isExternal,
        isInner = isInner,
        isCompanion = isCompanion,
        isData = isData,
        isFromSealedClass = isFromSealedClass,
        isFromEnumClass = isFromEnumClass,
        isFun = isFun,
        isRichError = isRichError,
    )
    return newStatus
}

fun FirDeclarationStatus.copyWithNewDefaults(
    visibility: Visibility? = this.visibility,
    modality: Modality? = this.modality,
    defaultVisibility: Visibility = this.defaultVisibility,
    defaultModality: Modality = this.defaultModality,
): FirDeclarationStatus {
    val newVisibility = visibility ?: this.visibility
    val newModality = modality ?: this.modality

    return when (this) {
        is FirCallableDeclarationStatus -> {
            val newStatus = when (this) {
                is FirResolvedCallableDeclarationStatus -> FirResolvedCallableDeclarationStatusWithAlteredDefaults(
                    newVisibility, newModality!!,
                    defaultVisibility, defaultModality,
                    effectiveVisibility
                )
                else -> FirCallableDeclarationStatusWithAlteredDefaults(
                    newVisibility, newModality,
                    defaultVisibility, defaultModality
                )
            }
            copyStatusAttributes(from = this, to = newStatus)
        }
        is FirClassLikeDeclarationStatus -> {
            val newStatus =when (this) {
                is FirResolvedClassLikeDeclarationStatus -> FirResolvedClassLikeDeclarationStatusWithAlteredDefaults(
                    newVisibility, newModality!!,
                    defaultVisibility, defaultModality,
                    effectiveVisibility
                )
                else -> FirClassLikeDeclarationStatusWithAlteredDefaults(
                    newVisibility, newModality,
                    defaultVisibility, defaultModality
                )
            }
            copyStatusAttributes(from = this, to = newStatus)
        }
        else -> shouldNotBeCalled()
    }
}

private fun copyStatusAttributes(
    from: FirCallableDeclarationStatus,
    to: FirCallableDeclarationStatusImpl,
    isExpect: Boolean = from.isExpect,
    isActual: Boolean = from.isActual,
    isOverride: Boolean = from.isOverride,
    isOperator: Boolean = from.isOperator,
    isInfix: Boolean = from.isInfix,
    isInline: Boolean = from.isInline,
    isTailRec: Boolean = from.isTailRec,
    isExternal: Boolean = from.isExternal,
    isConst: Boolean = from.isConst,
    isLateInit: Boolean = from.isLateInit,
    isSuspend: Boolean = from.isSuspend,
    isStatic: Boolean = from.isStatic,
    isFromSealedClass: Boolean = from.isFromSealedClass,
    isFromEnumClass: Boolean = from.isFromEnumClass,
    hasStableParameterNames: Boolean = from.hasStableParameterNames,
    returnValueStatus: ReturnValueStatus = from.returnValueStatus,
): FirCallableDeclarationStatusImpl {
    to.isExpect = isExpect
    to.isActual = isActual
    to.isOverride = isOverride
    to.isOperator = isOperator
    to.isInfix = isInfix
    to.isInline = isInline
    to.isTailRec = isTailRec
    to.isExternal = isExternal
    to.isConst = isConst
    to.isLateInit = isLateInit
    to.isSuspend = isSuspend
    to.isStatic = isStatic
    to.isFromSealedClass = isFromSealedClass
    to.isFromEnumClass = isFromEnumClass
    to.hasStableParameterNames = hasStableParameterNames
    to.returnValueStatus = returnValueStatus
    return to
}

private fun copyStatusAttributes(
    from: FirClassLikeDeclarationStatus,
    to: FirClassLikeDeclarationStatusImpl,
    isExpect: Boolean = from.isExpect,
    isActual: Boolean = from.isActual,
    isInline: Boolean = from.isInline,
    isValue: Boolean = from.isValue,
    isExternal: Boolean = from.isExternal,
    isInner: Boolean = from.isInner,
    isCompanion: Boolean = from.isCompanion,
    isData: Boolean = from.isData,
    isFromSealedClass: Boolean = from.isFromSealedClass,
    isFromEnumClass: Boolean = from.isFromEnumClass,
    isFun: Boolean = from.isFun,
    isRichError: Boolean = from.isRichError,
): FirClassLikeDeclarationStatusImpl {
    to.isExpect = isExpect
    to.isActual = isActual
    to.isInline = isInline
    to.isValue = isValue
    to.isExternal = isExternal
    to.isInner = isInner
    to.isCompanion = isCompanion
    to.isData = isData
    to.isFromSealedClass = isFromSealedClass
    to.isFromEnumClass = isFromEnumClass
    to.isFun = isFun
    to.isRichError = isRichError
    return to
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
        /*
         * If exception was caused by lack of static memory (e.g. `StackOverflowError`), the memory
         * would be not sufficient to load the `AnalysisExceptionsKt.class` containing the
         * `wrapIntoSourceCodeAnalysisExceptionIfNeeded` utility, which would lead to overriding
         * the original exception with `NoClassDefFoundError`.
         * So, considering that `wrapIntoSourceCodeAnalysisExceptionIfNeeded` skips VM errors anyway,
         * is's ok to check for it earlier.
         */
        if (throwable is VirtualMachineError) throw throwable
        throw throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element.source)
    }

    override fun handleExceptionOnFileAnalysis(file: FirFile, throwable: Throwable): Nothing {
        if (throwable is VirtualMachineError) throw throwable
        throw throwable.wrapIntoFileAnalysisExceptionIfNeeded(
            file.sourceFile?.path,
            file.source,
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

// Source element may be missing if the class came from a library
fun FirVariable.isEnumEntries(containingClass: FirClass): Boolean {
    return name == StandardNames.ENUM_ENTRIES && isSimpleStaticMemberOfEnumClass(containingClass)
}

fun FirVariable.isEnumEntries(containingClassSymbol: FirClassSymbol<*>): Boolean {
    return isEnumEntries(containingClassSymbol.fir)
}

fun FirCallableDeclaration.isGeneratedStaticEnumMember(containingClass: FirClass): Boolean {
    return when (this) {
        is FirVariable -> isEnumEntries(containingClass)
        is FirNamedFunction -> when (name) {
            StandardNames.ENUM_VALUES ->
                isSimpleStaticMemberOfEnumClass(containingClass) &&
                        valueParameters.isEmpty()
            StandardNames.ENUM_VALUE_OF ->
                isSimpleStaticMemberOfEnumClass(containingClass) &&
                        valueParameters.singleOrNull()?.returnTypeRef?.coneType?.isString == true
            else -> false
        }
        else -> false
    }
}

private fun FirCallableDeclaration.isSimpleStaticMemberOfEnumClass(containingClass: FirClass): Boolean {
    return isStatic &&
            containingClass.isEnumClass &&
            contextParameters.isEmpty() &&
            // Currently, companion block members can't have receivers, but maybe in the future they will.
            receiverParameter == null
}

val FirExpression.isArraySet: Boolean
    get() {
        val name = (this as? FirFunctionCall)?.calleeReference?.name ?: return false
        return origin == FirFunctionCallOrigin.Operator && name == OperatorNameConventions.SET
    }

val FirExpression.isStatementLikeExpression: Boolean
    get() = when (this) {
        is FirFunctionCall -> origin == FirFunctionCallOrigin.Operator && calleeReference.name in STATEMENT_LIKE_OPERATORS
        else -> isIndexedAssignment
    }

private val FirExpression.isIndexedAssignment: Boolean
    get() = this is FirBlock && statements.lastOrNull()?.source?.kind == KtFakeSourceElementKind.ImplicitUnit.IndexedAssignmentCoercion

fun FirBasedSymbol<*>.packageFqName(): FqName {
    return when (this) {
        is FirClassLikeSymbol<*> -> classId.packageFqName
        is FirPropertyAccessorSymbol -> propertySymbol.packageFqName()
        is FirCallableSymbol<*> -> callableId.packageName
        is FirScriptSymbol -> this.fqName.parent()
        is FirReplSnippetSymbol -> FqName.ROOT // TODO: add package FQN to snippet symbol (KT-74126)
        else -> error("No package fq name for $this")
    }
}

fun FirOperation.toAugmentedAssignSourceKind(): KtFakeSourceElementKind.DesugaredAugmentedAssign = when (this) {
    FirOperation.PLUS_ASSIGN -> KtFakeSourceElementKind.DesugaredPlusAssign
    FirOperation.MINUS_ASSIGN -> KtFakeSourceElementKind.DesugaredMinusAssign
    FirOperation.TIMES_ASSIGN -> KtFakeSourceElementKind.DesugaredTimesAssign
    FirOperation.DIV_ASSIGN -> KtFakeSourceElementKind.DesugaredDivAssign
    FirOperation.REM_ASSIGN -> KtFakeSourceElementKind.DesugaredRemAssign
    else -> error("Unexpected operator: $name")
}

fun buildWhenSubjectAccess(conditionSource: KtSourceElement, subjectVariable: FirVariable?): FirPropertyAccessExpression {
    return buildWhenSubjectExpression {
        source = conditionSource
        calleeReference = when (subjectVariable) {
            null -> buildErrorNamedReference {
                source = conditionSource.fakeElement(KtFakeSourceElementKind.UnresolvedWhenConditionSubject)
                name = SpecialNames.WHEN_SUBJECT
                diagnostic = ConeSimpleDiagnostic("No subject in when", DiagnosticKind.Other)
            }
            else -> buildResolvedNamedReference {
                source = conditionSource.fakeElement(KtFakeSourceElementKind.WhenCondition)
                resolvedSymbol = subjectVariable.symbol
                name = subjectVariable.name
            }
        }
    }
}

fun ConeKotlinType.toFirResolvedTypeRef(
    source: KtSourceElement? = null,
    delegatedTypeRef: FirTypeRef? = null
): FirResolvedTypeRef {
    return when (val lowerBoundIfFlexible = lowerBoundIfFlexible()) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source
            this.diagnostic = lowerBoundIfFlexible.diagnostic
            this.coneType = this@toFirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }
        else -> buildResolvedTypeRef {
            this.source = source
            this.coneType = this@toFirResolvedTypeRef
            this.delegatedTypeRef = delegatedTypeRef
        }
    }
}

val FirResolvedQualifier.ownTypeArguments: List<FirTypeProjection>
    get() = typeArguments.subList(0, typeArguments.size - (explicitParent?.typeArguments?.size ?: 0))
