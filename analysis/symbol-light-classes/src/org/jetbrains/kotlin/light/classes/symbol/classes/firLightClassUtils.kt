/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.analysis.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModuleOfTypeSafe
import org.jetbrains.kotlin.analysis.project.structure.NoCacheForModuleException
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import java.util.*

internal fun getOrCreateFirLightClass(classOrObject: KtClassOrObject): KtLightClass? =
    CachedValuesManager.getCachedValue(classOrObject) {
        CachedValueProvider.Result
            .create(
                createFirLightClassNoCache(classOrObject),
                classOrObject.project.createProjectWideOutOfBlockModificationTracker()
            )
    }

@OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
internal fun createFirLightClassNoCache(classOrObject: KtClassOrObject): KtLightClass? = hackyAllowRunningOnEdt {

    val containingFile = classOrObject.containingFile
    if (containingFile is KtCodeFragment) {
        // Avoid building light classes for code fragments
        return null
    }

    if (containingFile is KtFile && containingFile.isCompiled) return null

    if (classOrObject.shouldNotBeVisibleAsLightClass()) {
        return null
    }

    val anonymousObject = classOrObject.parent as? KtObjectLiteralExpression
    if (anonymousObject != null) {
        return analyseForLightClasses(anonymousObject) {
            anonymousObject.getAnonymousObjectSymbol().createLightClassNoCache(anonymousObject.manager)
        }
    }

    return when {
        classOrObject is KtEnumEntry -> lightClassForEnumEntry(classOrObject)
        classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) -> {
            analyseForLightClasses(classOrObject) {
                classOrObject.getNamedClassOrObjectSymbol()?.let { FirLightInlineClass(it, classOrObject.manager) }
            }
        }
        else -> {
            analyseForLightClasses(classOrObject) {
                classOrObject.getClassOrObjectSymbol().createLightClassNoCache(classOrObject.manager)
            }
        }
    }
}

internal fun KtClassOrObjectSymbol.createLightClassNoCache(manager: PsiManager): FirLightClassBase = when (this) {
    is KtAnonymousObjectSymbol -> FirLightAnonymousClassForSymbol(this, manager)
    is KtNamedClassOrObjectSymbol -> when (classKind) {
        KtClassKind.INTERFACE -> FirLightInterfaceClassSymbol(this, manager)
        KtClassKind.ANNOTATION_CLASS -> FirLightAnnotationClassSymbol(this, manager)
        else -> FirLightClassForSymbol(this, manager)
    }
}





private fun lightClassForEnumEntry(ktEnumEntry: KtEnumEntry): KtLightClass? {
    if (ktEnumEntry.body == null) return null

    val firClass = ktEnumEntry
        .containingClass()
        ?.let { getOrCreateFirLightClass(it) } as? FirLightClassForSymbol
        ?: return null

    val targetField = firClass.ownFields
        .firstOrNull { it is FirLightFieldForEnumEntry && it.kotlinOrigin == ktEnumEntry }
        ?: return null

    return (targetField as? FirLightFieldForEnumEntry)?.initializingClass as? KtLightClass
}

internal fun FirLightClassBase.createConstructors(
    declarations: Sequence<KtConstructorSymbol>,
    result: MutableList<KtLightMethod>
) {
    val constructors = declarations.toList()
    if (constructors.isEmpty()) {
        result.add(defaultConstructor())
        return
    }
    for (constructor in constructors) {
        if (constructor.isHiddenOrSynthetic(project)) continue
        result.add(
            FirLightConstructorForSymbol(
                constructorSymbol = constructor,
                lightMemberOrigin = null,
                containingClass = this@createConstructors,
                methodIndex = METHOD_INDEX_BASE
            )
        )
    }
    val primaryConstructor = constructors.singleOrNull { it.isPrimary }
    if (primaryConstructor != null && shouldGenerateNoArgOverload(primaryConstructor, constructors)) {
        result.add(
            noArgConstructor(primaryConstructor.visibility.externalDisplayName, METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR)
        )
    }
}

private fun FirLightClassBase.shouldGenerateNoArgOverload(
    primaryConstructor: KtConstructorSymbol,
    constructors: Iterable<KtConstructorSymbol>,
): Boolean {
    val classOrObject = kotlinOrigin ?: return false
    return !primaryConstructor.visibility.isPrivateOrPrivateToThis() &&
            !classOrObject.hasModifier(INNER_KEYWORD) && !isEnum &&
            !classOrObject.hasModifier(SEALED_KEYWORD) &&
            primaryConstructor.valueParameters.isNotEmpty() &&
            primaryConstructor.valueParameters.all { it.hasDefaultValue } &&
            constructors.none { it.valueParameters.isEmpty() } &&
            !primaryConstructor.hasJvmOverloadsAnnotation()
}

private fun FirLightClassBase.defaultConstructor(): KtLightMethod {
    val classOrObject = kotlinOrigin
    val visibility = when {
        classOrObject is KtObjectDeclaration || classOrObject?.hasModifier(SEALED_KEYWORD) == true || isEnum -> PsiModifier.PRIVATE
        classOrObject is KtEnumEntry -> PsiModifier.PACKAGE_LOCAL
        else -> PsiModifier.PUBLIC
    }
    return noArgConstructor(visibility, METHOD_INDEX_FOR_DEFAULT_CTOR)
}

private fun FirLightClassBase.noArgConstructor(
    visibility: String,
    methodIndex: Int,
): KtLightMethod {
    return FirLightNoArgConstructor(
        LightMemberOriginForDeclaration(
            originalElement = kotlinOrigin!!,
            originKind = JvmDeclarationOriginKind.OTHER,
            auxiliaryOriginalElement = kotlinOrigin as? KtDeclaration
        ),
        this,
        visibility,
        methodIndex
    )
}

internal fun FirLightClassBase.createMethods(
    declarations: Sequence<KtCallableSymbol>,
    result: MutableList<KtLightMethod>,
    isTopLevel: Boolean = false,
    suppressStaticForMethods: Boolean = false
) {
    val declarationGroups = declarations.groupBy { it is KtPropertySymbol && it.isFromPrimaryConstructor }

    fun handleDeclaration(declaration: KtCallableSymbol) {
        when (declaration) {
            is KtFunctionSymbol -> {
                // TODO: check if it has expect modifier
                if (declaration.hasReifiedParameters ||
                    declaration.isHiddenOrSynthetic(project)
                ) return

                var methodIndex = METHOD_INDEX_BASE
                result.add(
                    FirLightSimpleMethodForSymbol(
                        functionSymbol = declaration,
                        lightMemberOrigin = null,
                        containingClass = this@createMethods,
                        isTopLevel = isTopLevel,
                        methodIndex = methodIndex,
                        suppressStatic = suppressStaticForMethods
                    )
                )

                if (declaration.hasJvmOverloadsAnnotation()) {
                    val skipMask = BitSet(declaration.valueParameters.size)

                    for (i in declaration.valueParameters.size - 1 downTo 0) {

                        if (!declaration.valueParameters[i].hasDefaultValue) continue

                        skipMask.set(i)

                        result.add(
                            FirLightSimpleMethodForSymbol(
                                functionSymbol = declaration,
                                lightMemberOrigin = null,
                                containingClass = this@createMethods,
                                isTopLevel = isTopLevel,
                                methodIndex = methodIndex++,
                                argumentsSkipMask = skipMask.copy()
                            )
                        )
                    }
                }
            }
            is KtPropertySymbol -> createPropertyAccessors(result, declaration, isTopLevel)
            is KtConstructorSymbol -> error("Constructors should be handled separately and not passed to this function")
        }
    }

    // Regular members
    declarationGroups[false]?.forEach {
        handleDeclaration(it)
    }
    // Then, properties from the primary constructor parameters
    declarationGroups[true]?.forEach {
        handleDeclaration(it)
    }
}

internal fun FirLightClassBase.createPropertyAccessors(
    result: MutableList<KtLightMethod>,
    declaration: KtPropertySymbol,
    isTopLevel: Boolean,
    isMutable: Boolean = !declaration.isVal,
) {
    if (declaration is KtKotlinPropertySymbol && declaration.isConst) return

    if (declaration.visibility.isPrivateOrPrivateToThis() &&
        declaration.getter?.hasBody == false &&
        declaration.setter?.hasBody == false
    ) return

    if (declaration.hasJvmFieldAnnotation()) return

    fun KtPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
        if (declaration.hasReifiedParameters) return false
        if (!hasBody && visibility.isPrivateOrPrivateToThis()) return false
        if (declaration.isHiddenOrSynthetic(project, siteTarget)) return false
        if (isHiddenOrSynthetic(project)) return false
        return true
    }

    val originalElement = declaration.psi as? KtDeclaration

    val getter = declaration.getter?.takeIf {
        it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_GETTER)
    }

    if (getter != null) {
        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = getter.psi as? KtDeclaration
            )
        }

        result.add(
            FirLightAccessorMethodForSymbol(
                propertyAccessorSymbol = getter,
                containingPropertySymbol = declaration,
                lightMemberOrigin = lightMemberOrigin,
                containingClass = this@createPropertyAccessors,
                isTopLevel = isTopLevel
            )
        )
    }

    val setter = declaration.setter?.takeIf {
        !isAnnotationType && it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_SETTER)
    }

    if (isMutable && setter != null) {
        val lightMemberOrigin = originalElement?.let {
            LightMemberOriginForDeclaration(
                originalElement = it,
                originKind = JvmDeclarationOriginKind.OTHER,
                auxiliaryOriginalElement = setter.psi as? KtDeclaration
            )
        }
        result.add(
            FirLightAccessorMethodForSymbol(
                propertyAccessorSymbol = setter,
                containingPropertySymbol = declaration,
                lightMemberOrigin = lightMemberOrigin,
                containingClass = this@createPropertyAccessors,
                isTopLevel = isTopLevel
            )
        )
    }
}

internal fun FirLightClassBase.createField(
    declaration: KtPropertySymbol,
    nameGenerator: FirLightField.FieldNameGenerator,
    isTopLevel: Boolean,
    forceStatic: Boolean,
    takePropertyVisibility: Boolean,
    result: MutableList<KtLightField>
) {

    fun hasBackingField(property: KtPropertySymbol): Boolean = when (property) {
        is KtSyntheticJavaPropertySymbol -> true
        is KtKotlinPropertySymbol -> when {
            property.modality == Modality.ABSTRACT -> false
            property.isHiddenOrSynthetic(project) -> false
            property.isLateInit -> true
            property.isDelegatedProperty -> true
            property.isFromPrimaryConstructor -> true
            property.psi.let { it == null || it is KtParameter } -> true
            property.hasJvmSyntheticAnnotation(AnnotationUseSiteTarget.FIELD) -> false
            else -> property.hasBackingField
        }
    }

    if (!hasBackingField(declaration)) return

    val isDelegated = (declaration as? KtKotlinPropertySymbol)?.isDelegatedProperty == true
    val fieldName = nameGenerator.generateUniqueFieldName(
        declaration.name.asString() + (if (isDelegated) JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX else "")
    )

    result.add(
        FirLightFieldForPropertySymbol(
            propertySymbol = declaration,
            fieldName = fieldName,
            containingClass = this,
            lightMemberOrigin = null,
            isTopLevel = isTopLevel,
            forceStatic = forceStatic,
            takePropertyVisibility = takePropertyVisibility
        )
    )
}

internal fun FirLightClassBase.createInheritanceList(forExtendsList: Boolean, superTypes: List<KtType>): PsiReferenceList {

    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
        manager = manager,
        language = language,
        role = role
    )

    fun KtType.needToAddTypeIntoList(): Boolean {
        if (this !is KtNonErrorClassType) return false

        // Do not add redundant "extends java.lang.Object" anywhere
        if (this.classId == StandardClassIds.Any) return false

        // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
        if (isEnum && this.classId == StandardClassIds.Enum) return false

        val isInterfaceType =
            (this.classSymbol as? KtClassOrObjectSymbol)?.classKind == KtClassKind.INTERFACE

        return forExtendsList == !isInterfaceType
    }

    //TODO Add support for kotlin.collections.
    superTypes.asSequence()
        .filter { it.needToAddTypeIntoList() }
        .mapNotNull { type ->
            if (type !is KtNonErrorClassType) return@mapNotNull null
            analyzeWithSymbolAsContext(type.classSymbol) {
                mapSuperType(type, this@createInheritanceList, kotlinCollectionAsIs = true)
            }
        }
        .forEach { listBuilder.addReference(it) }

    return listBuilder
}

internal fun KtSymbolWithMembers.createInnerClasses(
    manager: PsiManager,
    containingClass: FirLightClassBase,
    classOrObject: KtClassOrObject?
): List<FirLightClassBase> {
    val result = ArrayList<FirLightClassBase>()

    // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
    // inner classes with null names can't be searched for and can't be used from java anyway
    // we can't prohibit creating light classes with null names either since they can contain members

    manager.project.analyzeWithSymbolAsContext(this) {
        getDeclaredMemberScope().getClassifierSymbols().filterIsInstance<KtNamedClassOrObjectSymbol>().mapTo(result) {
            it.createLightClassNoCache(manager)
        }
    }

    val jvmDefaultMode =
        classOrObject?.getKtModuleOfTypeSafe<KtSourceModule>()?.languageVersionSettings?.getFlag(JvmAnalysisFlags.jvmDefaultMode)
            ?: JvmDefaultMode.DEFAULT

    if (this is KtNamedClassOrObjectSymbol &&
        classOrObject?.hasInterfaceDefaultImpls == true &&
        jvmDefaultMode != JvmDefaultMode.ALL_INCOMPATIBLE
    ) {
        result.add(FirLightClassForInterfaceDefaultImpls(this, containingClass, manager))
    }
    return result
}

internal fun KtClassOrObject.checkIsInheritor(superClassOrigin: KtClassOrObject, checkDeep: Boolean): Boolean {
    val subClassOrigin = this

    fun KtAnalysisSession.check(): Boolean {
        val subClassSymbol = subClassOrigin.getClassOrObjectSymbol()
        val superClassSymbol = superClassOrigin.getClassOrObjectSymbol()

        if (subClassSymbol == superClassSymbol) return false

        return if (checkDeep) {
            subClassSymbol.isSubClassOf(superClassSymbol)
        } else {
            subClassSymbol.isDirectSubClassOf(superClassSymbol)
        }
    }

    // Due to the KT-51240 we cannot be sure which PSI element is better to analyse, so we try both.
    //
    // We specifically catch NoCacheForModuleException as a sign that LLFirSessionProvider.getModuleCache could not
    // find a cache for the passed module. This means that `module(subClassOrigin)` does not have `module(superClassOrigin)` as
    // its dependency, and we should try the other way around.
    //
    // Note, however, that the second call might also fail, since the modules can be completely independent.
    // We can only hope that, since this code is invoked from the light classes, the passed PSI classes are
    // relatively close to each other syntactically, meaning that there is some dependency between the modules
    // they reside in.
    //
    // TODO: Avoid this hack when KT-51240 is fixed
    return try {
        analyseForLightClasses(subClassOrigin) { check() }
    } catch (e: NoCacheForModuleException) {
        analyseForLightClasses(superClassOrigin) { check() }
    }
}

private val KtSymbolWithTypeParameters.hasReifiedParameters: Boolean
    get() = typeParameters.any { it.isReified }
