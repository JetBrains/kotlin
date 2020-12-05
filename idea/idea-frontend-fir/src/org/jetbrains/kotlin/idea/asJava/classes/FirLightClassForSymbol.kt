/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_DEFAULT_CTOR
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.asJava.classes.*
import org.jetbrains.kotlin.idea.asJava.classes.createInheritanceList
import org.jetbrains.kotlin.idea.asJava.classes.createInnerClasses
import org.jetbrains.kotlin.idea.asJava.classes.createMethods
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolVisibility
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.load.java.JvmAbi

internal open class FirLightClassForSymbol(
    private val classOrObjectSymbol: KtClassOrObjectSymbol,
    manager: PsiManager
) : FirLightClassForClassOrObjectSymbol(classOrObjectSymbol, manager) {

    init {
        require(classOrObjectSymbol.classKind != KtClassKind.INTERFACE && classOrObjectSymbol.classKind != KtClassKind.ANNOTATION_CLASS)
    }

    internal fun tryGetEffectiveVisibility(symbol: KtCallableSymbol): KtSymbolVisibility? {

        if (symbol !is KtPropertySymbol && symbol !is KtFunctionSymbol) return null

        var visibility = (symbol as? KtSymbolWithVisibility)?.visibility

        analyzeWithSymbolAsContext(symbol) {
            for (overriddenSymbol in symbol.getOverriddenSymbols(classOrObjectSymbol)) {
                val newVisibility = (overriddenSymbol as? KtSymbolWithVisibility)?.visibility
                if (newVisibility != null) {
                    visibility = newVisibility
                }
            }
        }

        return visibility
    }

    private val isTopLevel: Boolean = classOrObjectSymbol.symbolKind == KtSymbolKind.TOP_LEVEL

    private val _modifierList: PsiModifierList? by lazyPub {

        val modifiers = mutableSetOf(classOrObjectSymbol.computeVisibility(isTopLevel))
        classOrObjectSymbol.computeSimpleModality()?.run {
            modifiers.add(this)
        }
        if (!isTopLevel && !classOrObjectSymbol.isInner) {
            modifiers.add(PsiModifier.STATIC)
        }

        val annotations = classOrObjectSymbol.computeAnnotations(
            parent = this@FirLightClassForSymbol,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = null,
        )

        FirLightClassModifierList(this@FirLightClassForSymbol, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList? = _modifierList
    override fun getOwnFields(): List<KtLightField> = _ownFields
    override fun getOwnMethods(): List<PsiMethod> = _ownMethods
    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    private val _ownInnerClasses: List<FirLightClassForSymbol> by lazyPub {
        classOrObjectSymbol.createInnerClasses(manager)
    }

    override fun getOwnInnerClasses(): List<PsiClass> = _ownInnerClasses

    private val _extendsList by lazyPub { createInheritanceList(forExtendsList = true, classOrObjectSymbol.superTypes) }
    private val _implementsList by lazyPub { createInheritanceList(forExtendsList = false, classOrObjectSymbol.superTypes) }

    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val callableSymbols = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
            val visibleDeclarations = callableSymbols.applyIf(isEnum) {
                filterNot { function ->
                    function is KtFunctionSymbol && function.name.asString().let { it == "values" || it == "valueOf" }
                }
            }.applyIf(classOrObjectSymbol.classKind == KtClassKind.OBJECT) {
                filterNot {
                    it is KtPropertySymbol && it.isConst
                }
            }

            createMethods(visibleDeclarations, isTopLevel = false, result)
        }

        if (result.none { it.isConstructor }) {
            classOrObjectSymbol.primaryConstructor?.let {
                result.add(
                    FirLightConstructorForSymbol(
                        constructorSymbol = it,
                        lightMemberOrigin = null,
                        containingClass = this,
                        methodIndex = METHOD_INDEX_FOR_DEFAULT_CTOR
                    )
                )
            }
        }

        result
    }

    private fun addFieldsFromCompanionIfNeeded(result: MutableList<KtLightField>) {
        classOrObjectSymbol.companionObject?.run {
            analyzeWithSymbolAsContext(this) {
                getDeclaredMemberScope().getCallableSymbols()
                    .filterIsInstance<KtPropertySymbol>()
                    .filter { it.hasJvmFieldAnnotation() || it.hasJvmStaticAnnotation() || it.isConst }
                    .mapTo(result) {
                        FirLightFieldForPropertySymbol(
                            propertySymbol = it,
                            fieldName = it.name.asString(),
                            containingClass = this@FirLightClassForSymbol,
                            lightMemberOrigin = null,
                            isTopLevel = false,
                            forceStatic = !it.hasJvmStaticAnnotation(),
                            takePropertyVisibility = true
                        )
                    }
            }
        }
    }

    private fun addInstanceFieldIfNeeded(result: MutableList<KtLightField>) {
        val isNamedObject = classOrObjectSymbol.classKind == KtClassKind.OBJECT
        if (isNamedObject && classOrObjectSymbol.symbolKind != KtSymbolKind.LOCAL) {
            result.add(
                FirLightFieldForObjectSymbol(
                    objectSymbol = classOrObjectSymbol,
                    containingClass = this@FirLightClassForSymbol,
                    name = JvmAbi.INSTANCE_FIELD,
                    lightMemberOrigin = null
                )
            )
        }
    }

    private fun addPropertyBackingFields(result: MutableList<KtLightField>) {
        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val propertySymbols = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtPropertySymbol>()
                .applyIf(classOrObjectSymbol.classKind == KtClassKind.COMPANION_OBJECT) {
                    filterNot { it.hasJvmFieldAnnotation() || it.isConst }
                }

            val nameGenerator = FirLightField.FieldNameGenerator()
            val isObject = classOrObjectSymbol.classKind == KtClassKind.OBJECT
            val isCompanionObject = classOrObjectSymbol.classKind == KtClassKind.COMPANION_OBJECT

            for (propertySymbol in propertySymbols) {
                val isJvmField = propertySymbol.hasJvmFieldAnnotation()
                val isJvmStatic = propertySymbol.hasJvmStaticAnnotation()
                val forceStatic = isObject && (propertySymbol.isConst || isJvmStatic || isJvmField)
                val takePropertyVisibility = !isCompanionObject && (isJvmField || (isObject && isJvmStatic))

                createField(
                    declaration = propertySymbol,
                    nameGenerator = nameGenerator,
                    isTopLevel = false,
                    forceStatic = forceStatic,
                    takePropertyVisibility = takePropertyVisibility,
                    result = result
                )
            }

            if (isEnum) {
                classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                    .filterIsInstance<KtEnumEntrySymbol>()
                    .mapTo(result) { FirLightFieldForEnumEntry(it, this@FirLightClassForSymbol, null) }
            }
        }
    }

    private val _ownFields: List<KtLightField> by lazyPub {

        val result = mutableListOf<KtLightField>()

        addCompanionObjectFieldIfNeeded(result)
        addInstanceFieldIfNeeded(result)

        addFieldsFromCompanionIfNeeded(result)
        addPropertyBackingFields(result)

        result
    }

    override fun hashCode(): Int = classOrObjectSymbol.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is FirLightClassForSymbol && classOrObjectSymbol == other.classOrObjectSymbol)

    override fun isInterface(): Boolean = false

    override fun isAnnotationType(): Boolean = false

    override fun isEnum(): Boolean =
        classOrObjectSymbol.classKind == KtClassKind.ENUM_CLASS

    override fun copy(): FirLightClassForSymbol =
        FirLightClassForSymbol(classOrObjectSymbol, manager)
}