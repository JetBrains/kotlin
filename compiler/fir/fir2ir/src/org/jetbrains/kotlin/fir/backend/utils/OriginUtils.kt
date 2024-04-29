/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COMPONENT_PREFIX
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.resolve.isIterator
import org.jetbrains.kotlin.fir.resolve.isIteratorHasNext
import org.jetbrains.kotlin.fir.resolve.isIteratorNext
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

// ----------------------------------- declaration origins -----------------------------------

fun FirClass.irOrigin(c: Fir2IrComponents): IrDeclarationOrigin = when {
    c.firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    isJava -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    else -> when (val origin = origin) {
        is FirDeclarationOrigin.Plugin -> GeneratedByPlugin(origin.key)
        else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    }
}

fun FirDeclaration?.computeIrOrigin(
    predefinedOrigin: IrDeclarationOrigin? = null,
    parentOrigin: IrDeclarationOrigin? = null,
    fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
): IrDeclarationOrigin {
    if (this == null) {
        return predefinedOrigin ?: parentOrigin ?: IrDeclarationOrigin.DEFINED
    }

    val firOrigin = origin
    val computed = when {
        firOrigin is FirDeclarationOrigin.Plugin -> GeneratedByPlugin(firOrigin.key)

        this is FirValueParameter -> when (name) {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> IrDeclarationOrigin.UNDERSCORE_PARAMETER
            SpecialNames.DESTRUCT -> IrDeclarationOrigin.DESTRUCTURED_OBJECT_PARAMETER
            else -> null
        }

        this is FirCallableDeclaration -> when {
            fakeOverrideOwnerLookupTag != null && fakeOverrideOwnerLookupTag != containingClassLookupTag() -> IrDeclarationOrigin.FAKE_OVERRIDE
            isSubstitutionOrIntersectionOverride || isHiddenToOvercomeSignatureClash == true -> IrDeclarationOrigin.FAKE_OVERRIDE
            parentOrigin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB && symbol.isJavaOrEnhancement -> {
                IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            }
            symbol.origin is FirDeclarationOrigin.Plugin -> GeneratedByPlugin((symbol.origin as FirDeclarationOrigin.Plugin).key)
            else -> null
        }
        else -> null
    }

    return computed ?: predefinedOrigin ?: parentOrigin ?: IrDeclarationOrigin.DEFINED
}

// ----------------------------------- statement origins -----------------------------------

private val nameToOperationConventionOrigin: Map<Name, IrStatementOrigin> = mapOf(
    OperatorNameConventions.PLUS to IrStatementOrigin.PLUS,
    OperatorNameConventions.MINUS to IrStatementOrigin.MINUS,
    OperatorNameConventions.TIMES to IrStatementOrigin.MUL,
    OperatorNameConventions.DIV to IrStatementOrigin.DIV,
    OperatorNameConventions.MOD to IrStatementOrigin.PERC,
    OperatorNameConventions.REM to IrStatementOrigin.PERC,
    OperatorNameConventions.RANGE_TO to IrStatementOrigin.RANGE,
    OperatorNameConventions.RANGE_UNTIL to IrStatementOrigin.RANGE_UNTIL,
    OperatorNameConventions.CONTAINS to IrStatementOrigin.IN,
)

internal fun FirReference.statementOrigin(): IrStatementOrigin? = when (this) {
    is FirPropertyFromParameterResolvedNamedReference -> IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
    is FirResolvedNamedReference -> when (val symbol = resolvedSymbol) {
        is FirSyntheticPropertySymbol -> IrStatementOrigin.GET_PROPERTY
        is FirNamedFunctionSymbol -> when {
            symbol.callableId.isInvoke() ->
                IrStatementOrigin.INVOKE

            source?.kind == KtFakeSourceElementKind.DesugaredForLoop && symbol.callableId.isIteratorNext() ->
                IrStatementOrigin.FOR_LOOP_NEXT

            source?.kind == KtFakeSourceElementKind.DesugaredForLoop && symbol.callableId.isIteratorHasNext() ->
                IrStatementOrigin.FOR_LOOP_HAS_NEXT

            source?.kind == KtFakeSourceElementKind.DesugaredForLoop && symbol.callableId.isIterator() ->
                IrStatementOrigin.FOR_LOOP_ITERATOR

            source?.kind == KtFakeSourceElementKind.DesugaredInvertedContains ->
                IrStatementOrigin.NOT_IN

            source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement ->
                incOrDeclSourceKindToIrStatementOrigin[source?.kind]

            source?.kind is KtFakeSourceElementKind.DesugaredPrefixSecondGetReference ->
                incOrDeclSourceKindToIrStatementOrigin[source?.kind]

            source?.elementType == KtNodeTypes.OPERATION_REFERENCE ->
                nameToOperationConventionOrigin[symbol.callableId.callableName]

            source?.kind is KtFakeSourceElementKind.DesugaredComponentFunctionCall ->
                IrStatementOrigin.COMPONENT_N.withIndex(name.asString().removePrefix(DATA_CLASS_COMPONENT_PREFIX).toInt())

            source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign ->
                augmentedAssignSourceKindToIrStatementOrigin[source?.kind]

            source?.kind is KtFakeSourceElementKind.ArrayAccessNameReference -> when (name) {
                OperatorNameConventions.GET -> IrStatementOrigin.GET_ARRAY_ELEMENT
                OperatorNameConventions.SET -> IrStatementOrigin.EQ
                else -> null
            }

            else ->
                null
        }

        else -> null
    }

    else -> null
}

private typealias NameWithElementType = Pair<Name, IElementType>

private val PREFIX_POSTFIX_ORIGIN_MAP: Map<NameWithElementType, IrStatementOrigin> = hashMapOf(
    (OperatorNameConventions.INC to KtNodeTypes.PREFIX_EXPRESSION) to IrStatementOrigin.PREFIX_INCR,
    (OperatorNameConventions.INC to KtNodeTypes.POSTFIX_EXPRESSION) to IrStatementOrigin.POSTFIX_INCR,
    (OperatorNameConventions.DEC to KtNodeTypes.PREFIX_EXPRESSION) to IrStatementOrigin.PREFIX_DECR,
    (OperatorNameConventions.DEC to KtNodeTypes.POSTFIX_EXPRESSION) to IrStatementOrigin.POSTFIX_DECR,
)

fun FirVariableAssignment.getIrAssignmentOrigin(): IrStatementOrigin {
    incOrDeclSourceKindToIrStatementOrigin[source?.kind]?.let { return it }
    augmentedAssignSourceKindToIrStatementOrigin[source?.kind]?.let { return it }
    val callableName = getCallableNameFromIntClassIfAny() ?: return IrStatementOrigin.EQ
    PREFIX_POSTFIX_ORIGIN_MAP[callableName to source?.elementType]?.let { return it }

    val rValue = rValue as FirFunctionCall
    val kind = rValue.source?.kind

    return when (kind) {
        KtFakeSourceElementKind.DesugaredPrefixInc, KtFakeSourceElementKind.DesugaredPostfixInc -> IrStatementOrigin.PLUSEQ
        KtFakeSourceElementKind.DesugaredPrefixDec, KtFakeSourceElementKind.DesugaredPostfixDec -> IrStatementOrigin.MINUSEQ
        else -> IrStatementOrigin.EQ
    }
}

fun FirVariableAssignment.getIrPrefixPostfixOriginIfAny(): IrStatementOrigin? {
    val callableName = getCallableNameFromIntClassIfAny() ?: return null
    return PREFIX_POSTFIX_ORIGIN_MAP[callableName to source?.elementType]
}

private fun FirVariableAssignment.getCallableNameFromIntClassIfAny(): Name? {
    val calleeReferenceSymbol = calleeReference?.toResolvedCallableSymbol() ?: return null
    val rValue = rValue
    if (rValue is FirFunctionCall && calleeReferenceSymbol.callableId.isLocal) {
        val callableId = rValue.calleeReference.toResolvedCallableSymbol()?.callableId
        if (callableId?.classId == StandardClassIds.Int) {
            return callableId.callableName
        }
    }
    return null
}

val augmentedAssignSourceKindToIrStatementOrigin: Map<KtFakeSourceElementKind.DesugaredAugmentedAssign, IrStatementOrigin> = mapOf(
    KtFakeSourceElementKind.DesugaredPlusAssign to IrStatementOrigin.PLUSEQ,
    KtFakeSourceElementKind.DesugaredMinusAssign to IrStatementOrigin.MINUSEQ,
    KtFakeSourceElementKind.DesugaredTimesAssign to IrStatementOrigin.MULTEQ,
    KtFakeSourceElementKind.DesugaredDivAssign to IrStatementOrigin.DIVEQ,
    KtFakeSourceElementKind.DesugaredRemAssign to IrStatementOrigin.PERCEQ
)

val incOrDeclSourceKindToIrStatementOrigin: Map<KtFakeSourceElementKind, IrStatementOrigin> = mapOf(
    KtFakeSourceElementKind.DesugaredPrefixInc to IrStatementOrigin.PREFIX_INCR,
    KtFakeSourceElementKind.DesugaredPostfixInc to IrStatementOrigin.POSTFIX_INCR,
    KtFakeSourceElementKind.DesugaredPrefixDec to IrStatementOrigin.PREFIX_DECR,
    KtFakeSourceElementKind.DesugaredPostfixDec to IrStatementOrigin.POSTFIX_DECR,
    KtFakeSourceElementKind.DesugaredPrefixIncSecondGetReference to IrStatementOrigin.PREFIX_INCR,
    KtFakeSourceElementKind.DesugaredPrefixDecSecondGetReference to IrStatementOrigin.PREFIX_DECR
)
