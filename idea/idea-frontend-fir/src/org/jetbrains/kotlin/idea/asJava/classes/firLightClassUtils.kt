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
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.idea.asJava.*
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolVisibility
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import java.util.*

fun getOrCreateFirLightClass(classOrObject: KtClassOrObject): KtLightClass? =
    CachedValuesManager.getCachedValue(classOrObject) {
        CachedValueProvider.Result
            .create(
                createFirLightClassNoCache(classOrObject),
                KotlinModificationTrackerService.getInstance(classOrObject.project).outOfBlockModificationTracker
            )
    }

fun createFirLightClassNoCache(classOrObject: KtClassOrObject): KtLightClass? {

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
        return analyze(anonymousObject) {
            FirLightAnonymousClassForSymbol(anonymousObject.getAnonymousObjectSymbol(), anonymousObject.manager)
        }
    }

    return when {
        classOrObject is KtEnumEntry -> lightClassForEnumEntry(classOrObject)
        classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) -> return null //TODO

        else -> {
            analyze(classOrObject) {
                val symbol = classOrObject.getClassOrObjectSymbol()
                when (symbol.classKind) {
                    KtClassKind.INTERFACE -> FirLightInterfaceClassSymbol(symbol, classOrObject.manager)
                    KtClassKind.ANNOTATION_CLASS -> FirLightAnnotationClassSymbol(symbol, classOrObject.manager)
                    else -> FirLightClassForSymbol(symbol, classOrObject.manager)
                }
            }
        }
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

internal fun FirLightClassBase.createMethods(
    declarations: Sequence<KtCallableSymbol>,
    isTopLevel: Boolean,
    result: MutableList<KtLightMethod>
) {
    var methodIndex = METHOD_INDEX_BASE
    for (declaration in declarations) {

        if (declaration is KtFunctionSymbol && declaration.isInline) continue

        if (declaration is KtAnnotatedSymbol && declaration.hasJvmSyntheticAnnotation(annotationUseSiteTarget = null)) continue

        if (declaration is KtAnnotatedSymbol && declaration.isHiddenByDeprecation(annotationUseSiteTarget = null)) continue

        when (declaration) {
            is KtFunctionSymbol -> {
                result.add(
                    FirLightSimpleMethodForSymbol(
                        functionSymbol = declaration,
                        lightMemberOrigin = null,
                        containingClass = this@createMethods,
                        isTopLevel = isTopLevel,
                        methodIndex = methodIndex++
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
            is KtConstructorSymbol -> {
                result.add(
                    FirLightConstructorForSymbol(
                        constructorSymbol = declaration,
                        lightMemberOrigin = null,
                        containingClass = this@createMethods,
                        methodIndex++
                    )
                )
            }
            is KtPropertySymbol -> {

                if (declaration.hasJvmFieldAnnotation()) continue
                if (declaration.visibility == KtSymbolVisibility.PRIVATE) continue

                fun KtPropertyAccessorSymbol.needToCreateAccessor(siteTarget: AnnotationUseSiteTarget): Boolean {
                    if (isInline) return false
                    if (!hasBody && visibility == KtSymbolVisibility.PRIVATE) return false
                    return !declaration.hasJvmSyntheticAnnotation(siteTarget)
                            && !declaration.isHiddenByDeprecation(siteTarget)
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
    fun hasBackingField(property: KtPropertySymbol): Boolean {
        if (property.modality == KtCommonSymbolModality.ABSTRACT) return false
        if (property.isLateInit) return true
        //IS PARAMETER -> true
        if (property.getter == null && property.setter == null) return true
        if (property.hasJvmSyntheticAnnotation(AnnotationUseSiteTarget.FIELD)) return false
        return property.hasBackingField
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

internal fun FirLightClassBase.createInheritanceList(forExtendsList: Boolean, superTypes: List<KtType>): PsiReferenceList {

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
        .filterIsInstance<KtClassType>()
        .filter { it.needToAddTypeIntoList() }
        .mapNotNull { it.mapSupertype(this, kotlinCollectionAsIs = true) }
        .forEach { listBuilder.addReference(it) }

    return listBuilder
}

internal fun KtSymbolWithDeclarations.createInnerClasses(manager: PsiManager): List<FirLightClassForSymbol> {
    val result = ArrayList<FirLightClassForSymbol>()

    // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
    // inner classes with null names can't be searched for and can't be used from java anyway
    // we can't prohibit creating light classes with null names either since they can contain members

    analyzeWithSymbolAsContext(this) {
        getDeclaredMemberScope().getAllSymbols().filterIsInstance<KtClassOrObjectSymbol>().mapTo(result) {
            FirLightClassForSymbol(it, manager)
        }
    }

    //TODO
    //if (classOrObject.hasInterfaceDefaultImpls) {
    //    result.add(KtLightClassForInterfaceDefaultImpls(classOrObject))
    //}
    return result
}