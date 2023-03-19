/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.psiType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import java.util.*

internal fun <L : Any> L.invalidAccess(): Nothing =
    error("Cls delegate shouldn't be accessed for symbol light classes! Qualified name: ${javaClass.name}")

internal fun KtAnalysisSession.mapType(
    type: KtType,
    psiContext: PsiElement,
    mode: KtTypeMappingMode
): PsiClassType? {
    val psiTypeElement = type.asPsiTypeElement(
        psiContext,
        allowErrorTypes = true,
        mode,
    )
    return (psiTypeElement?.type as? PsiClassType)?.let {
        annotateByKtType(it, type, psiTypeElement, modifierListAsParent = null) as? PsiClassType
    }
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

internal fun KtSymbolWithVisibility.toPsiVisibilityForMember(): String = visibility.toPsiVisibilityForMember()

internal fun KtSymbolWithVisibility.toPsiVisibilityForClass(isNested: Boolean): String = visibility.toPsiVisibilityForClass(isNested)

private fun Visibility.toPsiVisibilityForMember(): String = when (this) {
    Visibilities.Private, Visibilities.PrivateToThis -> PsiModifier.PRIVATE
    Visibilities.Protected -> PsiModifier.PROTECTED
    else -> PsiModifier.PUBLIC
}

private fun Visibility.toPsiVisibilityForClass(isNested: Boolean): String = when (isNested) {
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

internal fun KtLightElement<*, *>.isOriginEquivalentTo(that: PsiElement?): Boolean {
    return kotlinOrigin?.isEquivalentTo(that) == true
}

internal fun KtAnalysisSession.getTypeNullability(ktType: KtType): NullabilityType {
    if (ktType.nullabilityType != NullabilityType.NotNull) return ktType.nullabilityType

    if (ktType.isUnit) return NullabilityType.NotNull

    if (ktType.isPrimitiveBacked) return NullabilityType.Unknown

    if (ktType is KtTypeParameterType) {
        if (ktType.isMarkedNullable) return NullabilityType.Nullable
        val subtypeOfNullableSuperType = ktType.symbol.upperBounds.all { upperBound -> upperBound.canBeNull }
        return if (!subtypeOfNullableSuperType) NullabilityType.NotNull else NullabilityType.Unknown
    }
    if (ktType !is KtNonErrorClassType) return NullabilityType.NotNull
    if (ktType.ownTypeArguments.any { it.type is KtClassErrorType }) return NullabilityType.NotNull
    if (ktType.classId.shortClassName.asString() == SpecialNames.ANONYMOUS_STRING) return NullabilityType.NotNull

    return ktType.nullabilityType
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

internal fun KtAnnotationValue.toAnnotationMemberValue(parent: PsiElement): PsiAnnotationMemberValue? = when (this) {
    is KtArrayAnnotationValue ->
        SymbolPsiArrayInitializerMemberValue(sourcePsi, parent) { arrayLiteralParent ->
            values.mapNotNull { element -> element.toAnnotationMemberValue(arrayLiteralParent) }
        }

    is KtAnnotationApplicationValue ->
        SymbolLightSimpleAnnotation(
            fqName = annotationValue.classId?.relativeClassName?.asString(),
            parent = parent,
            arguments = annotationValue.arguments,
            kotlinOrigin = annotationValue.psi,
        )

    is KtConstantAnnotationValue -> {
        this.constantValue.createPsiExpression(parent)?.let {
            if (it is PsiLiteral)
                SymbolPsiLiteral(sourcePsi, parent, it)
            else
                SymbolPsiExpression(sourcePsi, parent, it)
        }
    }

    is KtEnumEntryAnnotationValue -> asPsiReferenceExpression(parent)
    is KtKClassAnnotationValue -> toAnnotationMemberValue(parent)
    KtUnsupportedAnnotationValue -> null
}

private fun KtEnumEntryAnnotationValue.asPsiReferenceExpression(parent: PsiElement): SymbolPsiReference? {
    val fqName = this.callableId?.asSingleFqName()?.asString() ?: return null
    val psiReference = parent.project.withElementFactorySafe {
        createReferenceFromText(fqName, parent)
    } ?: return null

    return SymbolPsiReference(sourcePsi, parent, psiReference)
}

private fun KtKClassAnnotationValue.toAnnotationMemberValue(parent: PsiElement): PsiExpression? {
    val typeString = when (this) {
        is KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue -> classId.asSingleFqName().asString()
        is KtKClassAnnotationValue.KtLocalKClassAnnotationValue -> null
        is KtKClassAnnotationValue.KtErrorClassAnnotationValue -> unresolvedQualifierName
    } ?: return null

    val canonicalText = psiType(
        kotlinFqName = typeString,
        context = parent,
        boxPrimitiveType = false, /* TODO value.arrayNestedness > 0*/
    ).let(TypeConversionUtil::erasure).getCanonicalText(false)

    return parent.project.withElementFactorySafe {
        createExpressionFromText("$canonicalText.class", parent)
    }
}

private fun KtConstantValue.asStringForPsiLiteral(): String = when (val value = value) {
    is Char -> "'$value'"
    is String -> "\"${escapeString(value)}\""
    is Long -> "${value}L"
    is Float -> "${value}f"
    null -> "null"
    else -> value.toString()
}

internal fun KtConstantValue.createPsiExpression(parent: PsiElement): PsiExpression? {
    val asString = asStringForPsiLiteral()
    return parent.project.withElementFactorySafe {
        createExpressionFromText(asString, parent)
    }
}

private inline fun <T> Project.withElementFactorySafe(crossinline action: PsiElementFactory.() -> T): T? {
    val instance = PsiElementFactory.getInstance(this)
    return try {
        instance.action()
    } catch (_: IncorrectOperationException) {
        null
    }
}

internal fun BitSet.copy(): BitSet = clone() as BitSet

context(KtAnalysisSession)
internal fun <T : KtSymbol> KtSymbolPointer<T>.restoreSymbolOrThrowIfDisposed(): T =
    restoreSymbol()
        ?: buildErrorWithAttachment("${this::class} pointer already disposed") {
            withEntry("pointer", this@restoreSymbolOrThrowIfDisposed) { it.toString() }
        }

internal fun hasTypeParameters(
    ktModule: KtModule,
    declaration: KtTypeParameterListOwner?,
    declarationPointer: KtSymbolPointer<KtSymbolWithTypeParameters>,
): Boolean = declaration?.typeParameters?.isNotEmpty() ?: declarationPointer.withSymbol(ktModule) {
    it.typeParameters.isNotEmpty()
}

internal fun KtSymbolPointer<*>.isValid(ktModule: KtModule): Boolean = analyzeForLightClasses(ktModule) {
    restoreSymbol() != null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : KtSymbol> compareSymbolPointers(
    left: KtSymbolPointer<T>,
    right: KtSymbolPointer<T>,
): Boolean = left === right || left.pointsToTheSameSymbolAs(right)

internal inline fun <T : KtSymbol, R> KtSymbolPointer<T>.withSymbol(
    ktModule: KtModule,
    crossinline action: KtAnalysisSession.(T) -> R,
): R = analyzeForLightClasses(ktModule) { action(this, restoreSymbolOrThrowIfDisposed()) }

internal val KtPropertySymbol.isConstOrJvmField: Boolean get() = isConst || hasJvmFieldAnnotation()
internal val KtPropertySymbol.isConst: Boolean get() = (this as? KtKotlinPropertySymbol)?.isConst == true
internal val KtPropertySymbol.isLateInit: Boolean get() = (this as? KtKotlinPropertySymbol)?.isLateInit == true

internal inline fun <reified T> Collection<T>.toArrayIfNotEmptyOrDefault(default: Array<T>): Array<T> {
    return if (isNotEmpty()) toTypedArray() else default
}

internal inline fun <R : PsiElement, T> R.cachedValue(
    crossinline computer: () -> T,
): T = CachedValuesManager.getCachedValue(this) {
    CachedValueProvider.Result.createSingleDependency(computer(), project.createProjectWideOutOfBlockModificationTracker())
}
