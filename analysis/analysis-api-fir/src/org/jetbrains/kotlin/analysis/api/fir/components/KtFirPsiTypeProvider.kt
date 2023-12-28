/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.analysis.api.components.KtPsiTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeParameterImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import java.text.StringCharacterIterator

internal class KtFirPsiTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtPsiTypeProvider(), KtFirAnalysisSessionComponent {

    override fun asPsiTypeElement(
        type: KtType,
        useSitePosition: PsiElement,
        mode: KtTypeMappingMode,
        isAnnotationMethod: Boolean,
        allowErrorTypes: Boolean
    ): PsiTypeElement? {
        val coneType = type.coneType

        with(rootModuleSession.typeContext) {
            if (!allowErrorTypes && coneType.contains { it.isError() }) {
                return null
            }
        }

        if (!rootModuleSession.moduleData.platform.has<JvmPlatform>()) return null

        return coneType.simplifyType(rootModuleSession, useSitePosition)
            .asPsiTypeElement(rootModuleSession, mode.toTypeMappingMode(type, isAnnotationMethod), useSitePosition, allowErrorTypes)
    }

    private fun KtTypeMappingMode.toTypeMappingMode(type: KtType, isAnnotationMethod: Boolean): TypeMappingMode {
        require(type is KtFirType)
        return when (this) {
            KtTypeMappingMode.DEFAULT -> TypeMappingMode.DEFAULT
            KtTypeMappingMode.DEFAULT_UAST -> TypeMappingMode.DEFAULT_UAST
            KtTypeMappingMode.GENERIC_ARGUMENT -> TypeMappingMode.GENERIC_ARGUMENT
            KtTypeMappingMode.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
            KtTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS -> TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            KtTypeMappingMode.RETURN_TYPE_BOXED -> TypeMappingMode.RETURN_TYPE_BOXED
            KtTypeMappingMode.RETURN_TYPE -> {
                val expandedType = type.coneType.fullyExpandedType(rootModuleSession)
                rootModuleSession.jvmTypeMapper.typeContext.getOptimalModeForReturnType(expandedType, isAnnotationMethod)
            }
            KtTypeMappingMode.VALUE_PARAMETER -> {
                val expandedType = type.coneType.fullyExpandedType(rootModuleSession)
                rootModuleSession.jvmTypeMapper.typeContext.getOptimalModeForValueParameter(expandedType)
            }
        }
    }

    override fun asKtType(
        psiType: PsiType,
        useSitePosition: PsiElement,
    ): KtType? {
        val javaElementSourceFactory = JavaElementSourceFactory.getInstance(project)
        val javaType = JavaTypeImpl.create(psiType, javaElementSourceFactory.createTypeSource(psiType))

        val javaTypeRef = buildJavaTypeRef {
            //TODO KT-62351
            annotationBuilder = { emptyList() }
            type = javaType
        }

        val javaTypeParameterStack = MutableJavaTypeParameterStack()

        var psiClass = PsiTreeUtil.getContextOfType(useSitePosition, PsiClass::class.java, false)
        while (psiClass != null && psiClass.name == null) {
            psiClass = PsiTreeUtil.getContextOfType(psiClass, PsiClass::class.java, true)
        }
        if (psiClass != null) {
            val qualifiedName = psiClass.qualifiedName
            val packageName = (psiClass.containingFile as? PsiJavaFile)?.packageName ?: ""
            if (qualifiedName != null) {
                val javaClass = JavaClassImpl(javaElementSourceFactory.createPsiSource(psiClass))
                val relativeName = if (packageName.isEmpty()) qualifiedName else qualifiedName.substring(packageName.length + 1)
                val containingClassSymbol = rootModuleSession.javaSymbolProvider?.getClassLikeSymbolByClassId(
                    ClassId(
                        FqName(packageName),
                        FqName(relativeName.takeIf { !relativeName.isEmpty() } ?: SpecialNames.NO_NAME_PROVIDED.asString()),
                        PsiUtil.isLocalClass(psiClass)
                    ),
                    javaClass
                )
                if (containingClassSymbol != null) {
                    val member =
                        PsiTreeUtil.getContextOfType(useSitePosition, PsiTypeParameterListOwner::class.java, false, PsiClass::class.java)
                    if (member != null) {
                        val memberSymbol = containingClassSymbol.declarationSymbols.find { it.findPsi() == member } as? FirCallableSymbol<*>
                        if (memberSymbol != null) {
                            //typeParamSymbol.fir.source == null thus zip is required, see KT-62354
                            memberSymbol.typeParameterSymbols.zip(member.typeParameters).forEach { (typeParamSymbol, typeParam) ->
                                javaTypeParameterStack.addParameter(JavaTypeParameterImpl(typeParam), typeParamSymbol)
                            }
                        }
                    }

                    containingClassSymbol.typeParameterSymbols.zip(psiClass.typeParameters).forEach { (symbol, typeParameter) ->
                        javaTypeParameterStack.addParameter(JavaTypeParameterImpl(typeParameter), symbol)
                    }
                }
            }
        }
        val firTypeRef = javaTypeRef.resolveIfJavaType(analysisSession.useSiteSession, javaTypeParameterStack)
        val coneKotlinType = (firTypeRef as? FirResolvedTypeRef)?.type ?: return null
        return coneKotlinType.asKtType()
    }
}

private fun ConeKotlinType.simplifyType(
    session: FirSession,
    useSitePosition: PsiElement,
    visited: MutableSet<ConeKotlinType> = mutableSetOf()
): ConeKotlinType {
    // E.g., Wrapper<T> : Comparable<Wrapper<T>>
    if (!visited.add(this)) return this

    val substitutor = AnonymousTypesSubstitutor(session)
    val visibilityForApproximation = useSitePosition.visibilityForApproximation
    // TODO: See if the given [useSitePosition] is an `inline` method
    val isInlineFunction = false
    var currentType = this
    do {
        val oldType = currentType
        currentType = currentType.fullyExpandedType(session)
        if (currentType is ConeDynamicType) {
            return currentType
        }
        currentType = currentType.upperBoundIfFlexible()
        if (visibilityForApproximation != Visibilities.Local) {
            currentType = substitutor.substituteOrSelf(currentType)
        }
        val needLocalTypeApproximation = needLocalTypeApproximation(visibilityForApproximation, isInlineFunction, session, useSitePosition)
        // TODO: can we approximate local types in type arguments *selectively* ?
        currentType = PublicTypeApproximator.approximateTypeToPublicDenotable(currentType, session, needLocalTypeApproximation)
            ?: currentType

    } while (oldType !== currentType)
    if (typeArguments.isNotEmpty()) {
        currentType = currentType.withArguments { typeProjection ->
            typeProjection.replaceType(
                typeProjection.type?.simplifyType(session, useSitePosition, visited)
            )
        }
    }
    return currentType
}

private fun ConeKotlinType.needLocalTypeApproximation(
    visibilityForApproximation: Visibility,
    isInlineFunction: Boolean,
    session: FirSession,
    useSitePosition: PsiElement
): Boolean {
    if (!shouldApproximateAnonymousTypesOfNonLocalDeclaration(visibilityForApproximation, isInlineFunction)) return false
    val localTypes: List<ConeKotlinType> = if (isLocal(session)) listOf(this) else {
        typeArguments.mapNotNull {
            if (it is ConeKotlinTypeProjection && it.type.isLocal(session)) {
                it.type
            } else null
        }
    }
    val unavailableLocalTypes = localTypes.filterNot { it.isLocalButAvailableAtPosition(session, useSitePosition) }
    // Need to approximate if there are local types that are not available in this scope
    return localTypes.isNotEmpty() && unavailableLocalTypes.isNotEmpty()
}

// Mimic FirDeclaration.visibilityForApproximation
private val PsiElement.visibilityForApproximation: Visibility
    get() {
        if (this !is PsiMember) return Visibilities.Local
        val containerVisibility =
            if (parent is KtLightClassForFacade) Visibilities.Public
            else (parent as? PsiClass)?.visibility ?: Visibilities.Local
        val visibility = visibility
        if (containerVisibility == Visibilities.Local || visibility == Visibilities.Local) return Visibilities.Local
        if (containerVisibility == Visibilities.Private) return Visibilities.Private
        return visibility
    }

// Mimic JavaElementUtil#getVisibility
private val PsiModifierListOwner.visibility: Visibility
    get() {
        if (parents.any { it is PsiMethod }) return Visibilities.Local
        if (hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.Public
        }
        if (hasModifierProperty(PsiModifier.PRIVATE) || hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) {
            return Visibilities.Private
        }
        return if (language == JavaLanguage.INSTANCE) {
            when {
                hasModifierProperty(PsiModifier.PROTECTED) && hasModifierProperty(PsiModifier.STATIC) ->
                    JavaVisibilities.ProtectedStaticVisibility
                hasModifierProperty(PsiModifier.PROTECTED) ->
                    JavaVisibilities.ProtectedAndPackage
                else ->
                    JavaVisibilities.PackageVisibility
            }
        } else Visibilities.DEFAULT_VISIBILITY
    }

private fun ConeKotlinType.isLocal(session: FirSession): Boolean {
    return with(session.typeContext) {
        this@isLocal.typeConstructor().isLocalType()
    }
}

private fun ConeKotlinType.isLocalButAvailableAtPosition(
    session: FirSession,
    useSitePosition: PsiElement,
): Boolean {
    val localClassSymbol = this.toRegularClassSymbol(session) ?: return false
    val localPsi = localClassSymbol.source?.psi ?: return false
    val context = (useSitePosition as? KtLightElement<*, *>)?.kotlinOrigin ?: useSitePosition
    // Local type is available if it's inside the same context (containing declaration)
    // or containing declaration is inside the local type, e.g., a member of the local class
    return localPsi == context ||
            localPsi.parents.any { it == context } ||
            context.parents.any { it == localPsi }
}

private fun ConeKotlinType.asPsiTypeElement(
    session: FirSession,
    mode: TypeMappingMode,
    useSitePosition: PsiElement,
    allowErrorTypes: Boolean
): PsiTypeElement? {
    if (this !is SimpleTypeMarker) return null

    if (!allowErrorTypes && (this is ConeErrorType)) return null

    (this as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.lazyResolveToPhase(FirResolvePhase.STATUS)

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)

    //TODO Check thread safety
    session.jvmTypeMapper.mapType(this, mode, signatureWriter) {
        val containingFile = useSitePosition.containingKtFile
        // parameters for default setters does not have kotlin origin, but setter has
            ?: (useSitePosition as? KtLightParameter)?.parent?.parent?.containingKtFile
            ?: return@mapType null
        val correspondingImport = containingFile.findImportByAlias(it) ?: return@mapType null
        correspondingImport.importPath?.pathStr
    }

    val canonicalSignature = signatureWriter.toString()
    require(!canonicalSignature.contains(SpecialNames.ANONYMOUS_STRING))

    if (canonicalSignature.contains("L<error>")) return null
    if (canonicalSignature.contains(SpecialNames.NO_NAME_PROVIDED.asString())) return null

    val signature = StringCharacterIterator(canonicalSignature)
    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return null

    return ClsTypeElementImpl(useSitePosition, typeText, '\u0000')
}

private val PsiElement.containingKtFile: KtFile?
    get() = (this as? KtLightElement<*, *>)?.kotlinOrigin?.containingKtFile

private class AnonymousTypesSubstitutor(
    private val session: FirSession,
) : AbstractConeSubstitutor(session.typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeClassLikeType) return null

        val hasStableName = type.classId?.isLocal == true
        if (!hasStableName) {
            // Make sure we're not going to expand type argument over and over again.
            // If so, i.e., if there is a recursive type argument, return the current, non-null [type]
            // to prevent the following [substituteTypeOr*] from proceeding to its own (recursive) substitution.
            if (type.hasRecursiveTypeArgument()) return type
            // Return `null` means we will use [fir.resolve.substitution.Substitutors]'s [substituteRecursive]
            // that literally substitutes type arguments recursively.
            return null
        }

        val firClassNode = type.lookupTag.toSymbol(session) as? FirClassSymbol
        if (firClassNode != null) {
            firClassNode.resolvedSuperTypes.singleOrNull()?.let { return it }
        }

        return if (type.nullability.isNullable) session.builtinTypes.nullableAnyType.type
        else session.builtinTypes.anyType.type
    }

    private fun ConeKotlinType.hasRecursiveTypeArgument(
        visited: MutableSet<ConeKotlinType> = mutableSetOf()
    ): Boolean {
        if (typeArguments.isEmpty()) return false
        if (!visited.add(this)) return true
        for (projection in typeArguments) {
            // E.g., Test : Comparable<Test>
            val type = (projection as? ConeKotlinTypeProjection)?.type ?: continue
            // E.g., Comparable<Test>
            val newType = substituteOrNull(type) ?: continue
            // Visit new type: e.g., Test, as a type argument, is substituted with Comparable<Test>, again.
            if (newType.hasRecursiveTypeArgument(visited)) return true
        }
        return false
    }

}
