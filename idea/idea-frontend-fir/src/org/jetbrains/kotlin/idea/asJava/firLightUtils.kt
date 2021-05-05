/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isPrimitiveType
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclaration
import org.jetbrains.kotlin.idea.frontend.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import java.text.StringCharacterIterator
import java.util.*

internal fun <L : Any> L.invalidAccess(): Nothing =
    error("Cls delegate shouldn't be accessed for fir light classes! Qualified name: ${javaClass.name}")


private fun PsiElement.nonExistentType() = JavaPsiFacade.getElementFactory(project)
    .createTypeFromText("error.NonExistentClass", this)

internal fun KtTypedSymbol.asPsiType(parent: PsiElement, phase: FirResolvePhase): PsiType =
    annotatedType.asPsiType(this, parent, phase)

internal fun KtTypeAndAnnotations.asPsiType(
    context: KtSymbol,
    parent: PsiElement,
    phase: FirResolvePhase
): PsiType {
    val type = this.type
    require(type is KtFirType)
    require(context is KtFirSymbol<*>)
    val session = context.firRef.withFir(phase) { it.declarationSiteSession }
    return type.coneType.asPsiType(session, context.firRef.resolveState, TypeMappingMode.DEFAULT, parent)
}

internal fun KtNamedClassOrObjectSymbol.typeForClassSymbol(psiElement: PsiElement): PsiType {
    require(this is KtFirSymbol<*>)

    val types = analyzeWithSymbolAsContext(this) {
        this@typeForClassSymbol.buildSelfClassType()
    }
    require(types is KtFirType)

    val session = firRef.withFir { it.declarationSiteSession }
    return types.coneType.asPsiType(session, firRef.resolveState, TypeMappingMode.DEFAULT, psiElement)
}

private class AnonymousTypesSubstitutor(
    private val session: FirSession,
    private val state: FirModuleResolveState
) : AbstractConeSubstitutor() {
    override val typeInferenceContext: ConeInferenceContext
        get() = session.inferenceComponents.ctx

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {

        if (type !is ConeClassLikeType) return null

        val isAnonymous = type.classId.let { it?.shortClassName?.asString() == SpecialNames.ANONYMOUS }
        if (!isAnonymous) return null

        fun ConeClassLikeType.isNotInterface(): Boolean {
            val firClassNode = lookupTag.toSymbol(session)?.fir as? FirClass ?: return false
            return firClassNode.withFirDeclaration(state) { firSuperClass ->
                firSuperClass.classKind != ClassKind.INTERFACE
            }
        }

        val firClassNode = (type.lookupTag.toSymbol(session) as? FirClassSymbol)?.fir
        if (firClassNode != null) {
            val superTypesCones = firClassNode.withFirDeclaration(state, FirResolvePhase.SUPER_TYPES) {
                (it as? FirClass)?.superConeTypes
            }
            val superClass = superTypesCones?.firstOrNull { it.isNotInterface() }
            if (superClass != null) return superClass
        }

        return if (type.nullability.isNullable) session.builtinTypes.nullableAnyType.type
        else session.builtinTypes.anyType.type
    }
}


private fun ConeKotlinType.asPsiType(
    session: FirSession,
    state: FirModuleResolveState,
    mode: TypeMappingMode,
    psiContext: PsiElement,
): PsiType {

    if (this is ConeClassErrorType || this !is SimpleTypeMarker) return psiContext.nonExistentType()
    if (this.typeArguments.any { it is ConeClassErrorType }) return psiContext.nonExistentType()

    val correctedType = AnonymousTypesSubstitutor(session, state).substituteOrSelf(this)

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)

    //TODO Check thread safety
    session.jvmTypeMapper.mapType(correctedType, mode, signatureWriter)

    val canonicalSignature = signatureWriter.toString()

    if (canonicalSignature.contains("L<error>")) return psiContext.nonExistentType()
    require(!canonicalSignature.contains(SpecialNames.ANONYMOUS))

    val signature = StringCharacterIterator(canonicalSignature)
    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return psiContext.nonExistentType()

    val typeElement = ClsTypeElementImpl(psiContext, typeText, '\u0000')
    return typeElement.type
}

private fun mapSupertype(
    psiContext: PsiElement,
    session: FirSession,
    firResolvePhase: FirModuleResolveState,
    supertype: ConeKotlinType,
    kotlinCollectionAsIs: Boolean = false
) =
    supertype.asPsiType(
        session,
        firResolvePhase,
        if (kotlinCollectionAsIs) TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS else TypeMappingMode.SUPER_TYPE,
        psiContext
    ) as? PsiClassType

internal fun KtTypeAndAnnotations.mapSupertype(
    psiContext: PsiElement,
    kotlinCollectionAsIs: Boolean = false
): PsiClassType? = type.mapSupertype(psiContext, kotlinCollectionAsIs, emptyList())

internal fun KtType.mapSupertype(
    psiContext: PsiElement,
    kotlinCollectionAsIs: Boolean = false,
    annotations: List<KtAnnotationCall>
): PsiClassType? {
    if (this !is KtClassType) return null
    require(this is KtFirType)
    val contextSymbol = classSymbol
    require(contextSymbol is KtFirSymbol<*>)

    val session = contextSymbol.firRef.withFir { it.declarationSiteSession }

    return mapSupertype(
        psiContext,
        session,
        contextSymbol.firRef.resolveState,
        coneType,
        kotlinCollectionAsIs,
    )
}

internal enum class NullabilityType {
    Nullable,
    NotNull,
    Unknown
}

internal val KtType.nullabilityType: NullabilityType
    get() =
        (this as? KtTypeWithNullability)?.let {
            if (it.nullability == KtTypeNullability.NULLABLE) NullabilityType.Nullable else NullabilityType.NotNull
        } ?: NullabilityType.Unknown


internal fun FirMemberDeclaration.computeSimpleModality(): Set<String> {
    require(this !is FirConstructor)

    val modifier = when (modality) {
        Modality.FINAL -> PsiModifier.FINAL
        Modality.ABSTRACT -> PsiModifier.ABSTRACT
        Modality.SEALED -> PsiModifier.ABSTRACT
        else -> null
    }

    return modifier?.let { setOf(it) } ?: emptySet()
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
    }
}


internal fun KtSymbolWithVisibility.toPsiVisibilityForMember(isTopLevel: Boolean): String =
    visibility.toPsiVisibility(isTopLevel, forClass = false)

internal fun KtSymbolWithVisibility.toPsiVisibilityForClass(isTopLevel: Boolean): String =
    visibility.toPsiVisibility(isTopLevel, forClass = true)

internal fun Visibility.toPsiVisibilityForMember(isTopLevel: Boolean): String =
    toPsiVisibility(isTopLevel, forClass = false)

private fun Visibility.toPsiVisibility(isTopLevel: Boolean, forClass: Boolean): String = when (this) {
    // Top-level private class has PACKAGE_LOCAL visibility in Java
    // Nested private class has PRIVATE visibility
    Visibilities.Private, Visibilities.PrivateToThis ->
        if (forClass && isTopLevel) PsiModifier.PACKAGE_LOCAL else PsiModifier.PRIVATE
    Visibilities.Protected -> PsiModifier.PROTECTED
    else -> PsiModifier.PUBLIC
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

internal fun KtType.getTypeNullability(context: KtSymbol, phase: FirResolvePhase): NullabilityType {

    if (nullabilityType != NullabilityType.NotNull) return nullabilityType

    if (isUnit) return NullabilityType.NotNull

    require(this is KtFirType)
    require(context is KtFirSymbol<*>)

    if (coneType is ConeTypeParameterType) {
//        TODO Make supertype checking
//        val subtypeOfNullableSuperType = context.firRef.withFir(phase) {
//            it.session.typeCheckerContext.nullableAnyType().isSupertypeOf(it.session.typeCheckerContext, coneType)
//        }
//        if (!subtypeOfNullableSuperType) return NullabilityType.NotNull

        if (!coneType.isMarkedNullable) return NullabilityType.Unknown
        return NullabilityType.NotNull
    }

    val coneType = coneType as? ConeClassLikeType ?: return NullabilityType.NotNull

    if (!coneType.isPrimitiveType()) {
        return nullabilityType
    }

    if (coneType is ConeClassErrorType) return NullabilityType.NotNull
    if (coneType.typeArguments.any { it is ConeClassErrorType }) return NullabilityType.NotNull
    if (coneType.classId?.shortClassName?.asString() == SpecialNames.ANONYMOUS) return NullabilityType.NotNull

    val canonicalSignature = context.firRef.withFir(phase) {
        it.declarationSiteSession.jvmTypeMapper.mapType(coneType, TypeMappingMode.DEFAULT).descriptor
    }

    if (canonicalSignature == "[L<error>;") return NullabilityType.NotNull

    val isNotPrimitiveType = canonicalSignature.startsWith("L") || canonicalSignature.startsWith("[")

    return if (isNotPrimitiveType) NullabilityType.NotNull else NullabilityType.Unknown
}

internal val KtType.isUnit get() = isClassTypeWithClassId(DefaultTypeClassIds.UNIT)

internal fun KtType.isClassTypeWithClassId(classId: ClassId): Boolean {
    if (this !is KtClassType) return false
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

private fun KtSimpleConstantValue<*>.asStringForPsiLiteral(): String =
    when (val value = this.value) {
        is String -> "\"${escapeString(value)}\""
        is Long -> "${value}L"
        is Float -> "${value}f"
        else -> value?.toString() ?: "null"
    }

internal fun KtSimpleConstantValue<*>.createPsiLiteral(parent: PsiElement): PsiExpression? {
    val asString = asStringForPsiLiteral()
    val instance = PsiElementFactory.getInstance(parent.project)
    return try {
        instance.createExpressionFromText(asString, parent)
    } catch (_: IncorrectOperationException) {
        null
    }
}

internal inline fun <T> T.applyIf(`if`: Boolean, body: T.() -> T): T =
    if (`if`) body() else this

internal fun BitSet.copy(): BitSet = clone() as BitSet
