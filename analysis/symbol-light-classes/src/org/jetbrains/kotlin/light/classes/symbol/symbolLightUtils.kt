/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.psiType
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForInterface
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForInterfaceDefaultImpls
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.util.*

internal fun <L : Any> L.invalidAccess(): Nothing =
    error("Cls delegate shouldn't be accessed for symbol light classes! Qualified name: ${javaClass.name}")

internal fun KaSession.getContainingSymbolsWithSelf(symbol: KaDeclarationSymbol): Sequence<KaDeclarationSymbol> =
    generateSequence(symbol) { it.containingDeclaration }

internal fun KaSession.mapType(
    type: KaType,
    psiContext: PsiElement,
    mode: KaTypeMappingMode,
): PsiClassType? {
    val psiType = type.asPsiType(
        useSitePosition = psiContext,
        allowErrorTypes = true,
        mode = mode,
        allowNonJvmPlatforms = true,
    )

    return psiType as? PsiClassType
}

internal fun KaDeclarationSymbol.computeSimpleModality(): String? = when (modality) {
    KaSymbolModality.SEALED -> PsiModifier.ABSTRACT
    KaSymbolModality.FINAL -> PsiModifier.FINAL
    KaSymbolModality.ABSTRACT -> PsiModifier.ABSTRACT
    KaSymbolModality.OPEN -> null
}

internal fun KaSession.enumClassModality(symbol: KaClassSymbol): String? {
    if (symbol.memberScope.callables.any { it.modality == KaSymbolModality.ABSTRACT }) {
        return PsiModifier.ABSTRACT
    }

    if (symbol.staticDeclaredMemberScope.callables.none { it is KaEnumEntrySymbol && requiresSubClass(it) }) {
        return PsiModifier.FINAL
    }

    return null
}

private fun KaSession.requiresSubClass(symbol: KaEnumEntrySymbol): Boolean {
    val initializer = symbol.enumEntryInitializer ?: return false
    return initializer.combinedDeclaredMemberScope.declarations.any { it !is KaConstructorSymbol }
}

internal fun KaDeclarationSymbol.toPsiVisibilityForMember(): String = visibility.toPsiVisibilityForMember()

internal fun KaDeclarationSymbol.toPsiVisibilityForClass(isNested: Boolean): String = visibility.toPsiVisibilityForClass(isNested)

internal fun KaSymbolVisibility.toPsiVisibilityForMember(): String = when (this) {
    KaSymbolVisibility.PRIVATE -> PsiModifier.PRIVATE
    KaSymbolVisibility.PROTECTED -> PsiModifier.PROTECTED
    else -> PsiModifier.PUBLIC
}

private fun KaSymbolVisibility.toPsiVisibilityForClass(isNested: Boolean): String = when (isNested) {
    false -> when (this) {
        KaSymbolVisibility.PUBLIC,
        KaSymbolVisibility.PROTECTED,
        KaSymbolVisibility.LOCAL,
        KaSymbolVisibility.INTERNAL,
            -> PsiModifier.PUBLIC

        else -> PsiModifier.PACKAGE_LOCAL
    }

    true -> when (this) {
        KaSymbolVisibility.PUBLIC, KaSymbolVisibility.INTERNAL, KaSymbolVisibility.LOCAL -> PsiModifier.PUBLIC
        KaSymbolVisibility.PROTECTED -> PsiModifier.PROTECTED
        KaSymbolVisibility.PRIVATE -> PsiModifier.PRIVATE
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

internal fun KaSession.getTypeNullability(type: KaType): KaTypeNullability {
    if (type is KaClassErrorType) return KaTypeNullability.NON_NULLABLE

    val ktType = type.fullyExpandedType
    if (ktType.nullability != KaTypeNullability.NON_NULLABLE) return ktType.nullability

    if (ktType.isUnitType) return KaTypeNullability.NON_NULLABLE

    if (ktType.isPrimitiveBacked) return KaTypeNullability.UNKNOWN

    if (ktType is KaTypeParameterType) {
        if (ktType.isMarkedNullable) return KaTypeNullability.NULLABLE
        val subtypeOfNullableSuperType = ktType.symbol.upperBounds.all { upperBound -> upperBound.isNullable }
        return if (!subtypeOfNullableSuperType) KaTypeNullability.NON_NULLABLE else KaTypeNullability.UNKNOWN
    }

    if (ktType !is KaClassType) return KaTypeNullability.NON_NULLABLE
    if (ktType.typeArguments.any { it.type is KaClassErrorType }) return KaTypeNullability.NON_NULLABLE
    if (ktType.classId.shortClassName.asString() == SpecialNames.ANONYMOUS_STRING) return KaTypeNullability.NON_NULLABLE

    return ktType.nullability
}

internal val KaTypeNullability.asAnnotationQualifier: String?
    get() = when (this) {
        KaTypeNullability.NON_NULLABLE -> JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION
        KaTypeNullability.NULLABLE -> JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION
        else -> null
    }?.asString()

private fun escapeString(s: String): String = buildString {
    s.forEach {
        when (it) {
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            else -> if (it.code in 32..128) {
                append(it)
            } else {
                append("\\u%04X".format(it.code))
            }
        }
    }
}

internal fun AnnotationValue.toAnnotationMemberValue(parent: PsiElement): PsiAnnotationMemberValue? = when (this) {
    is AnnotationValue.Array -> {
        SymbolPsiArrayInitializerMemberValue(sourcePsi, parent) { arrayLiteralParent ->
            values.mapNotNull { element -> element.toAnnotationMemberValue(arrayLiteralParent) }
        }
    }

    is AnnotationValue.Annotation -> {
        SymbolLightSimpleAnnotation(
            fqName = classId?.asFqNameString(),
            parent = parent,
            arguments = normalizedArguments(),
            kotlinOrigin = sourcePsi,
        )
    }

    is AnnotationValue.Constant -> {
        constant.createPsiExpression(parent)?.let {
            when (it) {
                is PsiLiteralExpression -> SymbolPsiLiteral(sourcePsi, parent, it)
                else -> SymbolPsiExpression(sourcePsi, parent, it)
            }
        }
    }

    is AnnotationValue.EnumValue -> asPsiReferenceExpression(parent)

    is AnnotationValue.KClass -> toAnnotationMemberValue(parent)

    is AnnotationValue.Unsupported -> null
}

internal fun AnnotationValue.Annotation.normalizedArguments(): List<AnnotationArgument> {
    val args = arguments
    val ctorSymbolPointer = constructorSymbolPointer ?: return args
    val element = sourcePsi ?: return args // May work incorrectly. See KT-63568

    return analyzeForLightClasses(element) {
        val constructorSymbol = restoreSymbolOrThrowIfDisposed(ctorSymbolPointer)
        val params = constructorSymbol.valueParameters
        val missingVarargParameterName =
            params.singleOrNull { it.isVararg && !it.hasDefaultValue }?.name?.takeIf { name -> args.none { it.name == name } }
        if (missingVarargParameterName == null) args
        else args + AnnotationArgument(missingVarargParameterName, AnnotationValue.Array(emptyList(), null))
    }
}


private fun AnnotationValue.EnumValue.asPsiReferenceExpression(parent: PsiElement): SymbolPsiReference? {
    val fqName = this.callableId?.asSingleFqName()?.asString() ?: return null
    val psiReference = parent.project.withElementFactorySafe {
        createExpressionFromText(fqName, parent) as? PsiReferenceExpression
    } ?: return null

    return SymbolPsiReference(sourcePsi, parent, psiReference)
}

private fun AnnotationValue.KClass.toAnnotationMemberValue(parent: PsiElement): SymbolPsiClassObjectAccessExpression? {
    val typeString = classId?.asSingleFqName()?.asString() ?: return null

    val psiType = psiType(
        kotlinFqName = typeString,
        context = parent,
        boxPrimitiveType = false, /* TODO value.arrayNestedness > 0*/
    ).let(TypeConversionUtil::erasure)

    return SymbolPsiClassObjectAccessExpression(sourcePsi, parent, psiType)
}

private fun KaConstantValue.asStringForPsiExpression(): String =
    when (val value = value) {
        Double.NEGATIVE_INFINITY -> "-1.0 / 0.0"
        Double.NaN -> "0.0 / 0.0"
        Double.POSITIVE_INFINITY -> "1.0 / 0.0"
        Float.NEGATIVE_INFINITY -> "-1.0F / 0.0F"
        Float.NaN -> "0.0F / 0.0F"
        Float.POSITIVE_INFINITY -> "1.0F / 0.0F"
        '\'' -> "'\\''"
        is Char -> "'${escapeString(value.toString())}'"
        is String -> "\"${escapeString(value)}\""
        is Long -> "${value}L"
        is Float -> "${value}f"
        else -> value.toString()
    }

internal fun KaConstantValue.createPsiExpression(parent: PsiElement): PsiExpression? {
    val asString = asStringForPsiExpression()
    return parent.project.withElementFactorySafe {
        createExpressionFromText(asString, parent)
    }
}

internal inline fun <T> Project.withElementFactorySafe(crossinline action: PsiElementFactory.() -> T): T? {
    val instance = PsiElementFactory.getInstance(this)
    return try {
        instance.action()
    } catch (_: IncorrectOperationException) {
        null
    }
}

internal fun BitSet.copy(): BitSet = clone() as BitSet

internal fun <T : KaSymbol> KaSession.restoreSymbolOrThrowIfDisposed(pointer: KaSymbolPointer<T>): T =
    pointer.restoreSymbol()
        ?: errorWithAttachment("${pointer::class} pointer already disposed") {
            withEntry("pointer", pointer) { it.toString() }
        }

internal val SymbolLightClassBase.interfaceIfDefaultImpls: SymbolLightClassForInterface?
    get() = (this as? SymbolLightClassForInterfaceDefaultImpls)?.containingClass

internal val SymbolLightClassBase.isDefaultImplsForInterfaceWithTypeParameters: Boolean
    get() = interfaceIfDefaultImpls?.hasTypeParameters() == true

internal fun KaSymbolPointer<*>.isValid(ktModule: KaModule): Boolean = analyzeForLightClasses(ktModule) {
    restoreSymbol() != null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : KaSymbol> compareSymbolPointers(
    left: KaSymbolPointer<T>,
    right: KaSymbolPointer<T>,
): Boolean = left === right || left.pointsToTheSameSymbolAs(right)

internal inline fun <T : KaSymbol, R> KaSymbolPointer<T>.withSymbol(
    ktModule: KaModule,
    crossinline action: KaSession.(T) -> R,
): R = analyzeForLightClasses(ktModule) { action(this, restoreSymbolOrThrowIfDisposed(this@withSymbol)) }

internal val KaPropertySymbol.isConstOrJvmField: Boolean get() = isConst || isJvmField
internal val KaPropertySymbol.isJvmField: Boolean get() = backingFieldSymbol?.hasJvmFieldAnnotation() == true
internal val KaPropertySymbol.isConst: Boolean get() = (this as? KaKotlinPropertySymbol)?.isConst == true
internal val KaPropertySymbol.isLateInit: Boolean get() = (this as? KaKotlinPropertySymbol)?.isLateInit == true
internal val KaPropertySymbol.canHaveNonPrivateField: Boolean get() = isConstOrJvmField || isLateInit

internal inline fun <reified T> Collection<T>.toArrayIfNotEmptyOrDefault(default: Array<T>): Array<T> {
    return if (isNotEmpty()) toTypedArray() else default
}

internal inline fun <R : PsiElement, T> R.cachedValue(
    crossinline computer: () -> T,
): T = CachedValuesManager.getCachedValue(this) {
    val value = computer()
    val specialTrackers = (this as? SymbolLightClassBase)?.contentModificationTrackers()
    if (specialTrackers != null) {
        CachedValueProvider.Result.create(value, specialTrackers)
    } else {
        CachedValueProvider.Result.createSingleDependency(
            value,
            KotlinAsJavaSupportBase.getInstance(project).outOfBlockModificationTracker(this),
        )
    }
}
