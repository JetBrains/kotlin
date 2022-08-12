/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.psiType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolPsiArrayInitializerMemberValue
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolPsiExpression
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolPsiLiteral
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import java.util.*

internal fun <L : Any> L.invalidAccess(): Nothing =
    error("Cls delegate shouldn't be accessed for symbol light classes! Qualified name: ${javaClass.name}")


internal fun KtAnalysisSession.mapType(
    type: KtType,
    psiContext: PsiElement,
    mode: KtTypeMappingMode
): PsiClassType? {
    if (type is KtClassErrorType) return null
    val psiType = type.asPsiType(
        psiContext,
        mode,
    )
    return psiType as? PsiClassType
}


internal enum class NullabilityType {
    Nullable,
    NotNull,
    Unknown
}

//todo get rid of NullabilityType as it corresponds to KtTypeNullability
internal val KtType.nullabilityType: NullabilityType
    get() = when (nullability) {
        KtTypeNullability.NULLABLE -> NullabilityType.Nullable
        KtTypeNullability.NON_NULLABLE -> NullabilityType.NotNull
        KtTypeNullability.UNKNOWN -> NullabilityType.Unknown
    }

internal fun KtSymbolWithModality.computeSimpleModality(): String? = when (modality) {
    Modality.SEALED -> PsiModifier.ABSTRACT
    Modality.FINAL -> PsiModifier.FINAL
    Modality.ABSTRACT -> PsiModifier.ABSTRACT
    Modality.OPEN -> null
}

internal fun KtSymbolWithModality.computeModalityForMethod(
    isTopLevel: Boolean,
    suppressFinal: Boolean,
    result: MutableSet<String>
) {
    require(this !is KtClassLikeSymbol)

    computeSimpleModality()?.run {
        if (this != PsiModifier.FINAL || !suppressFinal) {
            result.add(this)
        }
    }

    if (this is KtFunctionSymbol && isExternal) {
        result.add(PsiModifier.NATIVE)
    }
    if (isTopLevel) {
        result.add(PsiModifier.STATIC)
        val needFinalModifier = when (this) {
            is KtPropertySymbol -> isDelegatedProperty || isVal
            else -> true
        }
        if (needFinalModifier) {
            result.add(PsiModifier.FINAL)
        }
    }
}

context(KtAnalysisSession)
internal fun PsiElement.tryGetEffectiveVisibility(symbol: KtCallableSymbol): Visibility? {
    if (symbol !is KtPropertySymbol && symbol !is KtFunctionSymbol) return null

    var visibility = (symbol as? KtSymbolWithVisibility)?.visibility

    for (overriddenSymbol in symbol.getAllOverriddenSymbols()) {
        val newVisibility = (overriddenSymbol as? KtSymbolWithVisibility)?.visibility
        if (newVisibility != null) {
            visibility = newVisibility
        }
    }

    return visibility
}

internal fun KtSymbolWithVisibility.toPsiVisibilityForMember(): String =
    visibility.toPsiVisibilityForMember()

internal fun KtSymbolWithVisibility.toPsiVisibilityForClass(isNested: Boolean): String =
    visibility.toPsiVisibilityForClass(isNested)

internal fun Visibility.toPsiVisibilityForMember(): String =
    when (this) {
        Visibilities.Private, Visibilities.PrivateToThis -> PsiModifier.PRIVATE
        Visibilities.Protected -> PsiModifier.PROTECTED
        else -> PsiModifier.PUBLIC
    }

private fun Visibility.toPsiVisibilityForClass(isNested: Boolean): String {
    return when (isNested) {
        false -> when (this) {
            Visibilities.Public,
            Visibilities.Protected,
            Visibilities.Local,
            Visibilities.Internal -> PsiModifier.PUBLIC

            else -> PsiModifier.PACKAGE_LOCAL
        }

        true -> when (this) {
            Visibilities.Public, Visibilities.Internal, Visibilities.Local -> PsiModifier.PUBLIC
            Visibilities.Protected -> PsiModifier.PROTECTED
            Visibilities.Private -> PsiModifier.PRIVATE
            else -> PsiModifier.PACKAGE_LOCAL
        }
    }
}

internal fun basicIsEquivalentTo(`this`: PsiElement?, that: PsiElement?): Boolean {
    if (`this` == null || that == null) return false
    if (`this` == that) return true

    if (`this` !is KtLightElement<*, *>) return false
    if (that !is KtLightElement<*, *>) return false
    if (`this`.kotlinOrigin?.isEquivalentTo(that.kotlinOrigin) == true) return true

    val thisMemberOrigin = (`this` as? KtLightMember<*>)?.lightMemberOrigin ?: return false
    if (thisMemberOrigin.isEquivalentTo(that)) return true

    val thatMemberOrigin = (that as? KtLightMember<*>)?.lightMemberOrigin ?: return false
    return thisMemberOrigin.isEquivalentTo(thatMemberOrigin)
}

internal fun KtAnalysisSession.getTypeNullability(ktType: KtType): NullabilityType {
    if (ktType.nullabilityType != NullabilityType.NotNull) return ktType.nullabilityType

    if (ktType.isUnit) return NullabilityType.NotNull

    if (ktType is KtTypeParameterType) {
        if (ktType.isMarkedNullable) return NullabilityType.Nullable
        val subtypeOfNullableSuperType = ktType.symbol.upperBounds.all { upperBound -> upperBound.canBeNull }
        return if (!subtypeOfNullableSuperType) NullabilityType.NotNull else NullabilityType.Unknown
    }
    if (ktType !is KtClassType) return NullabilityType.NotNull

    if (!ktType.isPrimitive) {
        return ktType.nullabilityType
    }

    if (ktType !is KtNonErrorClassType) return NullabilityType.NotNull
    if (ktType.typeArguments.any { it.type is KtClassErrorType }) return NullabilityType.NotNull
    if (ktType.classId.shortClassName.asString() == SpecialNames.ANONYMOUS_STRING) return NullabilityType.NotNull

    val canonicalSignature = ktType.mapTypeToJvmType().descriptor

    if (canonicalSignature == "[L<error>;") return NullabilityType.NotNull

    val isNotPrimitiveType = canonicalSignature.startsWith("L") || canonicalSignature.startsWith("[")

    return if (isNotPrimitiveType) NullabilityType.NotNull else NullabilityType.Unknown
}

internal val KtType.isUnit get() = isClassTypeWithClassId(DefaultTypeClassIds.UNIT)

internal fun KtType.isClassTypeWithClassId(classId: ClassId): Boolean {
    if (this !is KtNonErrorClassType) return false
    return this.classId == classId
}

private fun escapeString(str: String): String = buildString {
    str.forEach { char ->
        val escaped = when (char) {
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            '\"' -> "\\\""
            '\\' -> "\\\\"
            else -> "$char"
        }
        append(escaped)
    }
}

internal fun KtAnnotationValue.toAnnotationMemberValue(parent: PsiElement): PsiAnnotationMemberValue? {
    return when (this) {
        is KtArrayAnnotationValue ->
            SymbolPsiArrayInitializerMemberValue(sourcePsi, parent) { arrayLiteralParent ->
                values.mapNotNull { element -> element.toAnnotationMemberValue(arrayLiteralParent) }
            }

        is KtAnnotationApplicationValue ->
            SymbolLightSimpleAnnotation(
                annotationValue.classId?.relativeClassName?.asString(),
                parent,
                annotationValue.arguments,
                annotationValue.psi
            )

        is KtConstantAnnotationValue -> {
            this.constantValue.createPsiLiteral(parent)?.let {
                when (it) {
                    is PsiLiteral -> SymbolPsiLiteral(sourcePsi, parent, it)
                    else -> SymbolPsiExpression(sourcePsi, parent, it)
                }
            }
        }

        is KtEnumEntryAnnotationValue -> {
            val fqName = this.callableId?.asSingleFqName()?.asString() ?: return null
            val psiExpression = PsiElementFactory.getInstance(parent.project).createExpressionFromText(fqName, parent)
            SymbolPsiExpression(sourcePsi, parent, psiExpression)
        }

        KtUnsupportedAnnotationValue -> null
        is KtKClassAnnotationValue.KtErrorClassAnnotationValue -> null
        is KtKClassAnnotationValue.KtLocalKClassAnnotationValue -> null
        is KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue -> toAnnotationMemberValue(parent)
    }
}

private fun KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue.toAnnotationMemberValue(parent: PsiElement): PsiExpression? {
    val fqName = classId.asSingleFqName()
    val canonicalText = psiType(
        fqName.asString(), parent, boxPrimitiveType = false, /* TODO value.arrayNestedness > 0*/
    ).let(TypeConversionUtil::erasure).getCanonicalText(false)
    return try {
        PsiElementFactory.getInstance(parent.project).createExpressionFromText("$canonicalText.class", parent)
    } catch (_: IncorrectOperationException) {
        null
    }
}

private fun KtConstantValue.asStringForPsiLiteral(): String =
    when (val value = value) {
        is Char -> "'$value'"
        is String -> "\"${escapeString(value)}\""
        is Long -> "${value}L"
        is Float -> "${value}f"
        else -> value?.toString() ?: "null"
    }


internal fun KtConstantValue.createPsiLiteral(parent: PsiElement): PsiExpression? {
    val asString = asStringForPsiLiteral()
    return try {
        PsiElementFactory.getInstance(parent.project).createExpressionFromText(asString, parent)
    } catch (_: IncorrectOperationException) {
        null
    }
}


internal fun BitSet.copy(): BitSet = clone() as BitSet
