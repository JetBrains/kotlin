/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.isLocalClass
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.light.classes.symbol.annotations.annotateByKtType
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeParameterImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import org.jetbrains.kotlin.types.updateArgumentModeFromAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.org.objectweb.asm.Type

internal class KaFirJavaInteroperabilityComponent(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaJavaInteroperabilityComponent, KaFirSessionComponent {
    private val jvmTypeMapper: FirJvmTypeMapper by lazy {
        when {
            analysisSession.targetPlatform.has<JvmPlatform>() -> rootModuleSession.jvmTypeMapper
            else -> {
                // Type mapper is not registered in non-JVM sessions.
                // Here we use its custom instance for generating Java-like types in multiplatform source-sets.
                FirJvmTypeMapper(rootModuleSession)
            }
        }
    }

    override fun KaType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        preserveAnnotations: Boolean,
        allowNonJvmPlatforms: Boolean,
    ): PsiType? = withValidityAssertion {
        val coneType = this.coneType

        with(rootModuleSession.typeContext) {
            if (!allowErrorTypes && coneType.contains { it.isError() }) {
                return null
            }
        }

        if (!rootModuleSession.moduleData.platform.has<JvmPlatform>() && !allowNonJvmPlatforms) return null

        val typeElement = coneType.simplifyType(rootModuleSession, useSitePosition).asPsiTypeElement(
            mode = mode.toTypeMappingMode(this, isAnnotationMethod, suppressWildcards),
            useSitePosition = useSitePosition,
            allowErrorTypes = allowErrorTypes,
        ) ?: return null

        val psiType = typeElement.type
        if (!preserveAnnotations) return psiType

        return with(analysisSession) {
            annotateByKtType(
                psiType = psiType,
                ktType = this@asPsiType,
                annotationParent = typeElement,
            )
        }
    }

    private fun ConeKotlinType.asPsiTypeElement(
        mode: TypeMappingMode,
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
    ): PsiTypeElement? {
        if (this !is RigidTypeMarker) return null

        if (!allowErrorTypes && (this is ConeErrorType)) return null
        val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)

        //TODO Check thread safety
        jvmTypeMapper.mapType(this, mode, signatureWriter) {
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

        val signature = SignatureParsing.CharIterator(canonicalSignature)
        val typeInfo = SignatureParsing.parseTypeStringToTypeInfo(signature, StubBuildingVisitor.GUESSING_PROVIDER)
        val typeText = typeInfo.text() ?: return null

        return SyntheticTypeElement(useSitePosition, typeText)
    }

    private fun KaTypeMappingMode.toTypeMappingMode(
        type: KaType,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
    ): TypeMappingMode {
        require(type is KaFirType)

        val expandedType = type.coneType.fullyExpandedType(rootModuleSession)

        return when (this) {
            KaTypeMappingMode.DEFAULT -> TypeMappingMode.DEFAULT
            KaTypeMappingMode.DEFAULT_UAST -> TypeMappingMode.DEFAULT_UAST
            KaTypeMappingMode.GENERIC_ARGUMENT -> TypeMappingMode.GENERIC_ARGUMENT
            KaTypeMappingMode.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
            KaTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS -> TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            KaTypeMappingMode.RETURN_TYPE_BOXED -> TypeMappingMode.RETURN_TYPE_BOXED
            KaTypeMappingMode.RETURN_TYPE -> jvmTypeMapper.typeContext.getOptimalModeForReturnType(expandedType, isAnnotationMethod)
            KaTypeMappingMode.VALUE_PARAMETER -> jvmTypeMapper.typeContext.getOptimalModeForValueParameter(expandedType)
        }.let { typeMappingMode ->
            // Otherwise, i.e., if we won't skip type with no type arguments, flag overriding might bother a case like:
            // @JvmSuppressWildcards(false) Long -> java.lang.Long, not long, even though it should be no-op!
            if (expandedType.typeArguments.isEmpty())
                typeMappingMode
            else
                typeMappingMode.updateArgumentModeFromAnnotations(expandedType, jvmTypeMapper.typeContext, suppressWildcards)
        }
    }

    override fun PsiType.asKaType(useSitePosition: PsiElement): KaType? = withValidityAssertion {
        val javaElementSourceFactory = JavaElementSourceFactory.getInstance(project)
        val javaType = JavaTypeImpl.create(this, javaElementSourceFactory.createTypeSource(this))

        val javaTypeRef = buildJavaTypeRef {
            // Annotations are unused during `resolveIfJavaType`, so there is no need to provide something
            annotationBuilder = { emptyList() }
            type = javaType
        }

        val javaTypeParameterStack = MutableJavaTypeParameterStack()

        var psiClass = PsiTreeUtil.getContextOfType(useSitePosition, PsiClass::class.java, false)
        while (psiClass != null && psiClass.name == null || psiClass is PsiTypeParameter) {
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
                    val member = useSitePosition.parentsWithSelf
                        .filterNot { it is PsiTypeParameter }
                        .takeWhile { it !is PsiClass }
                        .firstIsInstanceOrNull<PsiTypeParameterListOwner>()

                    if (member != null) {
                        @OptIn(DirectDeclarationsAccess::class)
                        val memberSymbol = containingClassSymbol.declarationSymbols.find { it.findPsi(analysisSession.analysisScope) == member } as? FirCallableSymbol<*>
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
        val firTypeRef = javaTypeRef.resolveIfJavaType(analysisSession.firSession, javaTypeParameterStack, source = null)
        val coneKotlinType = (firTypeRef as? FirResolvedTypeRef)?.coneType ?: return null
        return coneKotlinType.asKtType()
    }

    override fun KaType.mapToJvmType(mode: TypeMappingMode): Type = withValidityAssertion {
        return jvmTypeMapper.mapType(coneType, mode, sw = null, unresolvedQualifierRemapper = null)
    }

    override val KaType.isPrimitiveBacked: Boolean
        get() = withValidityAssertion {
            if (analysisSession.targetPlatform.has<JvmPlatform>()) {
                return jvmTypeMapper.isPrimitiveBacked(coneType)
            }

            with(analysisSession) {
                if (!canBeNull) {
                    if (isPrimitive) {
                        return true
                    }

                    val classSymbol = symbol
                    if (classSymbol is KaNamedClassSymbol && classSymbol.isInline) {
                        val onlyProperty = classSymbol.memberScope.callables
                            .singleOrNull { it is KaPropertySymbol && it.isFromPrimaryConstructor }

                        if (onlyProperty != null && onlyProperty.returnType.isPrimitiveBacked) {
                            return true
                        }
                    }
                }
            }

            return false
        }

    override val PsiClass.namedClassSymbol: KaNamedClassSymbol?
        get() = withValidityAssertion {
            if (qualifiedName == null) return null
            if (this is PsiTypeParameter) return null
            if (this is KtLightClassMarker) return null
            if (isKotlinCompiledClass()) return null
            if (isLocalClass()) return null

            return KaFirPsiJavaClassSymbol(this, analysisSession)
        }

    private fun PsiClass.isKotlinCompiledClass() =
        this is ClsElementImpl && hasAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())

    override val PsiMember.callableSymbol: KaCallableSymbol?
        get() = withValidityAssertion {
            if (this !is PsiMethod && this !is PsiField) return null
            val containingClass = containingClass ?: return null
            val classSymbol = containingClass.namedClassSymbol ?: return null
            return with(analysisSession) {
                val combinedMemberScope = classSymbol.combinedDeclaredMemberScope
                if ((this@callableSymbol as? PsiMethod)?.isConstructor == true) {
                    combinedMemberScope.constructors.firstOrNull { it.psi == this@callableSymbol }
                } else {
                    val name = name?.let(Name::identifier) ?: return null
                    combinedMemberScope.callables(name).firstOrNull { it.psi == this@callableSymbol }
                }
            }
        }

    override val KaCallableSymbol.containingJvmClassName: String?
        get() = withValidityAssertion {
            val symbol = this@containingJvmClassName

            if (symbol.origin == KaSymbolOrigin.TYPEALIASED_CONSTRUCTOR) return null

            with(analysisSession) {
                val platform = symbol.containingModule.targetPlatform
                if (!platform.has<JvmPlatform>()) return null

                val containingSymbolOrSelf = when (symbol) {
                    is KaParameterSymbol -> symbol.containingDeclaration as? KaCallableSymbol ?: symbol
                    is KaPropertyAccessorSymbol -> symbol.containingDeclaration as? KaPropertySymbol ?: symbol
                    is KaBackingFieldSymbol -> symbol.owningProperty
                    else -> symbol
                }

                val firSymbol = containingSymbolOrSelf.firSymbol

                firSymbol.jvmClassNameIfDeserialized()?.let {
                    return it.fqNameForClassNameWithoutDollars.asString()
                }

                return if (containingSymbolOrSelf.isTopLevel) {
                    (firSymbol.fir.getContainingFile()?.psi as? KtFile)
                        ?.takeUnless { it.isScript() }
                        ?.javaFileFacadeFqName?.asString()
                } else {
                    val classId = (containingSymbolOrSelf as? KaConstructorSymbol)?.containingClassId
                        ?: containingSymbolOrSelf.callableId?.classId
                    classId?.takeUnless { it.shortClassName.isSpecial }
                        ?.asFqNameString()
                }
            }
        }

    override val KaPropertySymbol.javaGetterName: Name
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)
            if (this is KaFirSyntheticJavaPropertySymbol) {
                return javaGetterSymbol.name
            }

            val firProperty = firSymbol.fir
            requireIsInstance<FirProperty>(firProperty)

            return getJvmName(firProperty, isSetter = false)
        }

    override val KaPropertySymbol.javaSetterName: Name?
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)
            if (this is KaFirSyntheticJavaPropertySymbol) {
                return javaSetterSymbol?.name
            }

            val firProperty = firSymbol.fir
            requireIsInstance<FirProperty>(firProperty)

            if (firProperty.isVal) return null

            return getJvmName(firProperty, isSetter = true)
        }

    private fun getJvmName(property: FirProperty, isSetter: Boolean): Name {
        if (property.backingField?.symbol?.hasAnnotation(JvmStandardClassIds.Annotations.JvmField, analysisSession.firSession) == true) {
            return property.name
        }
        return Name.identifier(getJvmNameAsString(property, isSetter))
    }

    private fun getJvmNameAsString(property: FirProperty, isSetter: Boolean): String {
        val useSiteTarget = if (isSetter) AnnotationUseSiteTarget.PROPERTY_SETTER else AnnotationUseSiteTarget.PROPERTY_GETTER
        val jvmNameFromProperty = property.getJvmNameFromAnnotation(analysisSession.firSession, useSiteTarget)
        if (jvmNameFromProperty != null) {
            return jvmNameFromProperty
        }

        val accessor = if (isSetter) property.setter else property.getter
        val jvmNameFromAccessor = accessor?.getJvmNameFromAnnotation(analysisSession.firSession)
        if (jvmNameFromAccessor != null) {
            return jvmNameFromAccessor
        }

        val identifier = property.name.identifier
        return if (isSetter) JvmAbi.setterName(identifier) else JvmAbi.getterName(identifier)
    }
}

private fun ConeKotlinType.simplifyType(
    session: FirSession,
    useSitePosition: PsiElement,
    visited: MutableSet<ConeKotlinType> = mutableSetOf(),
): ConeKotlinType {
    // E.g., Wrapper<T> : Comparable<Wrapper<T>>
    if (!visited.add(this)) return this

    val substitutor = AnonymousTypesSubstitutor(session)
    val visibilityForApproximation = useSitePosition.visibilityForApproximation
    // TODO: See if the given [useSitePosition] is an `inline` method
    val isInlineFunction = false
    var currentType = this
    do {
        ProgressManager.checkCanceled()

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
    useSitePosition: PsiElement,
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

private class SyntheticTypeElement(parent: PsiElement, typeText: String) : ClsTypeElementImpl(parent, typeText, '\u0000'), SyntheticElement

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
        firClassNode?.resolvedSuperTypes?.singleOrNull()?.let { return it }

        return if (type.isMarkedNullable) session.builtinTypes.nullableAnyType.coneType
        else session.builtinTypes.anyType.coneType
    }

    private fun ConeKotlinType.hasRecursiveTypeArgument(
        visited: MutableSet<ConeKotlinType> = mutableSetOf(),
    ): Boolean {
        if (typeArguments.isEmpty()) return false
        if (!visited.add(this)) return true

        ProgressManager.checkCanceled()

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
