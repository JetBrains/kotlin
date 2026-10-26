/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsElementImpl
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.bridges.lightClassBridge
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.findSyntheticJavaPropertyAccessor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.coneType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isLocalClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import org.jetbrains.kotlin.types.updateArgumentModeFromAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.requireIsInstance

internal class KaFirJavaInteroperabilityComponent(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaInternalsJavaInteroperabilityComponent {
    private val jvmTypeMapper: FirJvmTypeMapper by lazy { createFirJvmTypeMapper(analysisSession) }

    /**
     * [useSitePosition] is used as pure psi, so there is no need to validate it.
     * Also, it might represent some complex expressions like light classes or UAST
     */
    override fun asPsiType(
        type: KaType,
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        preserveAnnotations: Boolean,
        allowNonJvmPlatforms: Boolean,
    ): PsiType? = withValidityAssertion {
        val coneType = type.coneType

        with(rootModuleSession.typeContext) {
            if (!allowErrorTypes && coneType.contains { it.isError() }) {
                return null
            }
        }

        if (!rootModuleSession.moduleData.platform.has<JvmPlatform>() && !allowNonJvmPlatforms) return null

        if (mode == KaTypeMappingMode.FUNCTION_RETURN_TYPE && !isAnnotationMethod && coneType.isUnit) {
            // Here we approximate the JVM backend logic from `MethodSignatureMapper#hasVoidReturnType`.
            // But we do it only in the special type mapping mode `FUNCTION_RETURN_TYPE` because it is not applicable in other cases.
            return PsiTypes.voidType()
        }

        val mappingMode = mode.toTypeMappingMode(type, isAnnotationMethod, suppressWildcards)
        val typeElement = coneType.simplifyType(rootModuleSession, useSitePosition).asPsiTypeElement(
            mode = mappingMode,
            useSitePosition = useSitePosition,
            allowErrorTypes = allowErrorTypes,
        ) ?: return null

        val psiType = typeElement.type
        if (!preserveAnnotations) return psiType

        return with(analysisSession) {
            annotateByKtType(
                psiType = psiType,
                ktType = type,
                annotationParent = typeElement,
                inferNullabilityForTypeArguments = !mappingMode.ignoreTypeArgumentsBounds,
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
            KaTypeMappingMode.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE_AS_IS
            KaTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS -> TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            KaTypeMappingMode.RETURN_TYPE_BOXED -> TypeMappingMode.RETURN_TYPE_BOXED
            KaTypeMappingMode.RETURN_TYPE, KaTypeMappingMode.FUNCTION_RETURN_TYPE ->
                jvmTypeMapper.typeContext.getOptimalModeForReturnType(expandedType, isAnnotationMethod)

            KaTypeMappingMode.VALUE_PARAMETER, KaTypeMappingMode.VALUE_PARAMETER_BOXED -> {
                val mappingMode = jvmTypeMapper.typeContext.getOptimalModeForValueParameter(expandedType)
                if (this == KaTypeMappingMode.VALUE_PARAMETER_BOXED) {
                    mappingMode.wrapInlineClassesMode()
                } else {
                    mappingMode
                }
            }
        }.let { typeMappingMode ->
            // Otherwise, i.e., if we won't skip type with no type arguments, flag overriding might bother a case like:
            // @JvmSuppressWildcards(false) Long -> java.lang.Long, not long, even though it should be no-op!
            if (expandedType.typeArguments.isEmpty())
                typeMappingMode
            else
                typeMappingMode.updateArgumentModeFromAnnotations(expandedType, jvmTypeMapper.typeContext, suppressWildcards)
        }
    }

    /**
     * [useSitePosition] is used as pure psi, so there is no need to validate it.
     * Also, it might represent some complex expressions like light classes or UAST
     */
    override fun asKaType(psiType: PsiType, useSitePosition: PsiElement): KaType? = withValidityAssertion {
        val javaElementSourceFactory = JavaElementSourceFactory.getInstance(project)
        val javaType = JavaTypeImpl.create(psiType, javaElementSourceFactory.createTypeSource(psiType))

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
                            memberSymbol.typeParameterSymbols.zip(member.typeParameters).forEach { [typeParamSymbol, typeParam] ->
                                javaTypeParameterStack.addParameter(JavaTypeParameterImpl(typeParam), typeParamSymbol)
                            }
                        }
                    }

                    containingClassSymbol.typeParameterSymbols.zip(psiClass.typeParameters).forEach { [symbol, typeParameter] ->
                        javaTypeParameterStack.addParameter(JavaTypeParameterImpl(typeParameter), symbol)
                    }
                }
            }
        }
        val firTypeRef = javaTypeRef.resolveIfJavaType(analysisSession.firSession, javaTypeParameterStack, source = null)
        val coneKotlinType = (firTypeRef as? FirResolvedTypeRef)?.coneType ?: return null
        return coneKotlinType.asKaType()
    }

    override fun mapToJvmTypeDescriptor(type: KaType): String {
        return jvmTypeMapper.mapType(type.coneType, TypeMappingMode.DEFAULT, sw = null, unresolvedQualifierRemapper = null).descriptor
    }

    override fun isPrimitiveBacked(type: KaType): Boolean = withValidityAssertion {
        if (analysisSession.targetPlatform.has<JvmPlatform>()) {
            return jvmTypeMapper.isPrimitiveBacked(type.coneType)
        }

        with(analysisSession) {
            if (!type.isNullable) {
                if (type.isPrimitive) {
                    return true
                }

                val classSymbol = type.symbol
                if (classSymbol is KaNamedClassSymbol && classSymbol.isInline) {
                    val onlyProperty = classSymbol.memberScope.callables
                        .singleOrNull { it is KaKotlinPropertySymbol && it.primaryConstructorParameter != null }

                    if (onlyProperty != null && isPrimitiveBacked(onlyProperty.returnType)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    override fun asPsiClass(classSymbol: KaClassSymbol): PsiClass? = withValidityAssertion {
        lightClassBridge.getLightClass(classSymbol, session = analysisSession)
    }

    override fun asFacadePsiClass(fileSymbol: KaFileSymbol): PsiClass? = withValidityAssertion {
        lightClassBridge.getLightFacade(fileSymbol, session = analysisSession)
    }

    override fun asFacadePsiClass(scriptSymbol: KaScriptSymbol): PsiClass? = withValidityAssertion {
        lightClassBridge.getLightFacade(scriptSymbol, session = analysisSession)
    }

    override fun asPsiMethods(functionSymbol: KaFunctionSymbol): List<PsiMethod> = withValidityAssertion {
        lightClassBridge.getLightClassMethods(functionSymbol, session = analysisSession)
    }

    override fun asPsiTypeParameters(typeParameterSymbol: KaTypeParameterSymbol): List<PsiTypeParameter> = withValidityAssertion {
        lightClassBridge.getLightClassTypeParameter(typeParameterSymbol, session = analysisSession)
    }

    override fun asPsiParameters(parameterSymbol: KaParameterSymbol): List<PsiParameter> = withValidityAssertion {
        lightClassBridge.getLightClassParameters(parameterSymbol, session = analysisSession)
    }

    override fun asPsiField(backingFieldSymbol: KaBackingFieldSymbol): PsiField? = withValidityAssertion {
        lightClassBridge.getLightClassBackingField(backingFieldSymbol, session = analysisSession)
    }

    override fun asPsiField(classSymbol: KaClassSymbol): PsiField? = withValidityAssertion {
        lightClassBridge.getLightClassBackingField(classSymbol, session = analysisSession)
    }

    override fun asPsiField(enumEntrySymbol: KaEnumEntrySymbol): PsiEnumConstant? = withValidityAssertion {
        lightClassBridge.getLightClassBackingField(enumEntrySymbol, session = analysisSession) as? PsiEnumConstant
    }

    override fun namedClassSymbol(psiClass: PsiClass): KaNamedClassSymbol? = psiClass.withPsiValidityAssertion {
        if (psiClass is PsiTypeParameter) return null
        if (psiClass is KtLightElement<*, *>) return null
        if (psiClass.qualifiedName == null) return null

        if (psiClass.isKotlinCompiledClass()) return null
        if (psiClass.isLocalClass()) return null

        return KaFirPsiJavaClassSymbol(psiClass, analysisSession)
    }

    private fun PsiClass.isKotlinCompiledClass() =
        this is ClsElementImpl && hasAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())

    override fun callableSymbol(psiMember: PsiMember): KaCallableSymbol? = psiMember.withPsiValidityAssertion {
        if (psiMember !is PsiMethod && psiMember !is PsiField) return null
        if (psiMember is KtLightElement<*, *>) return null

        val containingClass = psiMember.containingClass ?: return null
        val classSymbol = namedClassSymbol(containingClass) ?: return null
        return with(analysisSession) {
            val combinedMemberScope = classSymbol.combinedDeclaredMemberScope
            if ((psiMember as? PsiMethod)?.isConstructor == true) {
                combinedMemberScope.constructors.firstOrNull { it.psi == psiMember }
            } else {
                val name = psiMember.name?.let(Name::identifier) ?: return null
                combinedMemberScope.callables(name).firstOrNull { it.psi == psiMember }
                    ?: findJavaAccessorMethodBySyntheticProperty(psiMember, name, combinedMemberScope)
            }
        }
    }

    /**
     * Finds a [KaCallableSymbol] for a Java accessor method that implements a Kotlin property.
     *
     * When a Java class implements a Kotlin interface property via getFoo/setFoo methods,
     * these methods are represented as a [KaSyntheticJavaPropertySymbol] in the scope.
     * This function retrieves the underlying getter/setter symbol for such cases.
     */
    context(_: KaFirSession)
    private fun findJavaAccessorMethodBySyntheticProperty(psiMember: PsiMember, name: Name, scope: KaScope): KaCallableSymbol? {
        return scope.findSyntheticJavaPropertyAccessor(name) { propertySymbol, accessorKind, _ ->
            accessorKind.getJavaAccessorSymbol(propertySymbol)?.takeIf { it.psi == psiMember }
        }
    }

    override fun containingJvmClassName(symbol: KaCallableSymbol): String? = withValidityAssertion {
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

    override fun javaGetterName(symbol: KaPropertySymbol): Name = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        if (symbol is KaFirSyntheticJavaPropertySymbol) {
            return symbol.javaGetterSymbol.name
        }

        val firProperty = symbol.firSymbol.fir
        requireIsInstance<FirProperty>(firProperty)

        return getJvmName(firProperty, isSetter = false)
    }

    override fun javaSetterName(symbol: KaPropertySymbol): Name? = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        if (symbol is KaFirSyntheticJavaPropertySymbol) {
            return symbol.javaSetterSymbol?.name
        }

        val firProperty = symbol.firSymbol.fir
        requireIsInstance<FirProperty>(firProperty)

        if (firProperty.isVal) return null

        return getJvmName(firProperty, isSetter = true)
    }

    private fun getJvmName(property: FirProperty, isSetter: Boolean): Name {
        if (property.backingField?.symbol?.hasAnnotation(JvmStandardClassIds.Annotations.JvmField, analysisSession.firSession) == true) {
            return property.name
        }

        val nameString = getJvmNameAsString(property, isSetter) ?: return SpecialNames.NO_NAME_PROVIDED
        return Name.identifier(nameString)
    }

    private fun getJvmNameAsString(property: FirProperty, isSetter: Boolean): String? {
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

        val identifier = property.name.takeUnless { it.isSpecial }?.identifier ?: return null
        return if (isSetter) JvmAbi.setterName(identifier) else JvmAbi.getterName(identifier)
    }

    override fun javaMethodName(function: KaFunctionSymbol): String? = withValidityAssertion {
        context(analysisSession) {
            val defaultName = defaultJavaMethodName(function) ?: return null
            lightClassBridge.computeJavaMethodName(
                symbol = function,
                defaultName = defaultName,
                ignoreInlineClassMangling = false
            )?.takeIf(StringUtil::isJavaIdentifier)
        }
    }

    /**
     * The Java method name for [function] as if there were no [JvmName] annotation and no `internal` mangling.
     *
     * @return `null` if [function] has no method visible from Java
     */
    context(_: KaSession)
    private fun defaultJavaMethodName(function: KaFunctionSymbol): String? = when (function) {
        is KaNamedFunctionSymbol -> {
            // Names of local declarations are invented during lowering, so they cannot be computed from a symbol
            if (function.location == KaSymbolLocation.LOCAL) {
                null
            } else {
                function.name.asString()
            }
        }

        is KaPropertyAccessorSymbol -> defaultJavaAccessorName(function)

        // Constructors are neither renamed nor mangled, and there is no JVM method behind a SAM constructor
        is KaAnonymousFunctionSymbol, is KaConstructorSymbol, is KaSamConstructorSymbol -> null
    }

    /**
     * @see defaultJavaMethodName
     */
    context(_: KaSession)
    private fun defaultJavaAccessorName(accessor: KaPropertyAccessorSymbol): String? {
        val property = accessor.containingDeclaration as? KaPropertySymbol ?: return null
        if (property is KaSyntheticJavaPropertySymbol) {
            val javaAccessor = when (accessor) {
                is KaPropertyGetterSymbol -> property.javaGetterSymbol
                is KaPropertySetterSymbol -> property.javaSetterSymbol
            }

            return javaAccessor?.name?.asString()
        }

        // A `const` property, as well as a property with a `JvmField` backing field, is materialized as a field,
        // so it has no accessor methods
        if (property is KaKotlinPropertySymbol && property.isConst) {
            return null
        }

        if (property.backingFieldSymbol?.annotations?.contains(JvmStandardClassIds.Annotations.JvmField) == true) {
            return null
        }

        val identifier = property.name.identifierOrNullIfSpecial ?: return null
        if (with(lightClassBridge) { property.hasPrefixlessAccessorNames }) {
            return identifier
        }

        return if (accessor is KaPropertyGetterSymbol) {
            JvmAbi.getterName(identifier)
        } else {
            JvmAbi.setterName(identifier)
        }
    }

}

private class SyntheticTypeElement(parent: PsiElement, typeText: String) : ClsTypeElementImpl(parent, typeText, '\u0000'), SyntheticElement

private val PsiElement.containingKtFile: KtFile?
    get() = (this as? KtLightElement<*, *>)?.kotlinOrigin?.containingKtFile
