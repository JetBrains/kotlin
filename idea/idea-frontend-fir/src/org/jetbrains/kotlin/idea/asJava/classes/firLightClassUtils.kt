/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.idea.asJava.*
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.trackers.createProjectWideOutOfBlockModificationTracker
import java.util.*

fun getOrCreateFirLightClass(classOrObject: KtClassOrObject): KtLightClass? =
    CachedValuesManager.getCachedValue(classOrObject) {
        CachedValueProvider.Result
            .create(
                createFirLightClassNoCache(classOrObject),
                classOrObject.project.createProjectWideOutOfBlockModificationTracker()
            )
    }

@OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
fun createFirLightClassNoCache(classOrObject: KtClassOrObject): KtLightClass? = hackyAllowRunningOnEdt {

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
            FirLightAnonymousClassForSymbol(anonymousObject.getAnonymousObjectSymbol(), anonymousObject.manager)
        }
    }

    return when {
        classOrObject is KtEnumEntry -> lightClassForEnumEntry(classOrObject)
        classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) -> return null //TODO

        else -> {
            analyseForLightClasses(classOrObject) {
                when (val symbol = classOrObject.getClassOrObjectSymbol()) {
                    is KtAnonymousObjectSymbol -> FirLightAnonymousClassForSymbol(symbol, classOrObject.manager)
                    is KtNamedClassOrObjectSymbol -> when (symbol.classKind) {
                        KtClassKind.INTERFACE -> FirLightInterfaceClassSymbol(symbol, classOrObject.manager)
                        KtClassKind.ANNOTATION_CLASS -> FirLightAnnotationClassSymbol(symbol, classOrObject.manager)
                        else -> FirLightClassForSymbol(symbol, classOrObject.manager)
                    }
                }
            }
        }
    }
}

fun getOrCreateFirLightFacade(
    ktFiles: List<KtFile>,
    facadeClassFqName: FqName,
): FirLightClassForFacade? {
    val firstFile = ktFiles.firstOrNull() ?: return null
    //TODO Make caching keyed by all files
    return CachedValuesManager.getCachedValue(firstFile) {
        CachedValueProvider.Result
            .create(
                getOrCreateFirLightFacadeNoCache(ktFiles, facadeClassFqName),
                KotlinModificationTrackerService.getInstance(firstFile.project).outOfBlockModificationTracker
            )
    }
}

fun getOrCreateFirLightFacadeNoCache(
    ktFiles: List<KtFile>,
    facadeClassFqName: FqName,
): FirLightClassForFacade? {
    val firstFile = ktFiles.firstOrNull() ?: return null
    return FirLightClassForFacade(firstFile.manager, facadeClassFqName, ktFiles)
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

internal fun FirLightClassForSymbol.createConstructors(
    declarations: Sequence<KtConstructorSymbol>,
    result: MutableList<KtLightMethod>
) {
    for (declaration in declarations) {
        if (declaration.isHiddenOrSynthetic()) continue
        result.add(
            FirLightConstructorForSymbol(
                constructorSymbol = declaration,
                lightMemberOrigin = null,
                containingClass = this@createConstructors,
                methodIndex = METHOD_INDEX_BASE
            )
        )
    }
}

internal fun FirLightClassBase.createMethods(
    declarations: Sequence<KtCallableSymbol>,
    result: MutableList<KtLightMethod>,
    isTopLevel: Boolean = false,
    suppressStaticForMethods: Boolean = false
) {
    for (declaration in declarations) {

        when (declaration) {
            is KtFunctionSymbol -> {
                if (declaration.isInline || declaration.isHiddenOrSynthetic()) continue

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
            is KtPropertySymbol -> {

                if (declaration is KtKotlinPropertySymbol && declaration.isConst) continue

                if (declaration.visibility.isPrivateOrPrivateToThis() &&
                    declaration.getter?.hasBody == false &&
                    declaration.setter?.hasBody == false
                ) continue

                if (declaration.hasJvmFieldAnnotation()) continue

                fun KtPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
                    if (isInline) return false
                    if (!hasBody && visibility.isPrivateOrPrivateToThis()) return false
                    if (declaration.isHiddenOrSynthetic(siteTarget)) return false
                    if (isHiddenOrSynthetic()) return false
                    return true
                }

                val getter = declaration.getter?.takeIf {
                    it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_GETTER)
                }

                if (getter != null) {
                    result.add(
                        FirLightAccessorMethodForSymbol(
                            propertyAccessorSymbol = getter,
                            containingPropertySymbol = declaration,
                            lightMemberOrigin = null,
                            containingClass = this@createMethods,
                            isTopLevel = isTopLevel
                        )
                    )
                }

                val setter = declaration.setter?.takeIf {
                    !isAnnotationType && it.needToCreateAccessor(AnnotationUseSiteTarget.PROPERTY_SETTER)
                }

                if (setter != null) {
                    result.add(
                        FirLightAccessorMethodForSymbol(
                            propertyAccessorSymbol = setter,
                            containingPropertySymbol = declaration,
                            lightMemberOrigin = null,
                            containingClass = this@createMethods,
                            isTopLevel = isTopLevel
                        )
                    )
                }
            }
            is KtConstructorSymbol -> error("Constructors should be handled separately and not passed to this function")
        }
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
            property.isHiddenOrSynthetic() -> false
            property.isLateInit -> true
            //TODO Fix it when KtFirConstructorValueParameterSymbol be ready
            property.psi.let { it == null || it is KtParameter } -> true
            property.hasJvmSyntheticAnnotation(AnnotationUseSiteTarget.FIELD) -> false
            else -> property.hasBackingField
        }
    }

    if (!hasBackingField(declaration)) return

    result.add(
        FirLightFieldForPropertySymbol(
            propertySymbol = declaration,
            fieldName = nameGenerator.generateUniqueFieldName(declaration.name.asString()),
            containingClass = this,
            lightMemberOrigin = null,
            isTopLevel = isTopLevel,
            forceStatic = forceStatic,
            takePropertyVisibility = takePropertyVisibility
        )
    )
}

internal fun FirLightClassBase.createInheritanceList(forExtendsList: Boolean, superTypes: List<KtTypeAndAnnotations>): PsiReferenceList {

    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        kotlinOrigin = kotlinOrigin?.getSuperTypeList(),
        manager = manager,
        language = language,
        role = role
    )

    fun KtType.needToAddTypeIntoList(): Boolean {
        if (this !is KtClassType) return false

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
        .filter { it.type.needToAddTypeIntoList() }
        .mapNotNull { it.mapSupertype(this, kotlinCollectionAsIs = true) }
        .forEach { listBuilder.addReference(it) }

    return listBuilder
}

internal fun KtSymbolWithMembers.createInnerClasses(manager: PsiManager): List<FirLightClassForSymbol> {
    val result = ArrayList<FirLightClassForSymbol>()

    // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
    // inner classes with null names can't be searched for and can't be used from java anyway
    // we can't prohibit creating light classes with null names either since they can contain members

    analyzeWithSymbolAsContext(this) {
        getDeclaredMemberScope().getClassifierSymbols().filterIsInstance<KtNamedClassOrObjectSymbol>().mapTo(result) {
            FirLightClassForSymbol(it, manager)
        }
    }

    //TODO
    //if (classOrObject.hasInterfaceDefaultImpls) {
    //    result.add(KtLightClassForInterfaceDefaultImpls(classOrObject))
    //}
    return result
}

internal fun KtClassOrObject.checkIsInheritor(baseClassOrigin: KtClassOrObject, checkDeep: Boolean): Boolean {
    return analyseForLightClasses(this) {
        val thisSymbol = this@checkIsInheritor.getNamedClassOrObjectSymbol()
        val baseSymbol = baseClassOrigin.getNamedClassOrObjectSymbol()

        if (thisSymbol == baseSymbol) return@analyseForLightClasses false

        val baseType = baseSymbol.buildSelfClassType()

        if (checkDeep) {
            thisSymbol.buildSelfClassType().isSubTypeOf(baseType)
        } else {
            thisSymbol.superTypes.any { baseType.isEqualTo(it.type) }
        }
    }
}
