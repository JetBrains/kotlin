/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NON_ORIGIN_METHOD
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.light.classes.symbol.classes.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal open class FirLightClassForSymbol(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : FirLightClassForClassOrObjectSymbol(classOrObjectSymbol, manager) {

    init {
        require(classOrObjectSymbol.classKind != KtClassKind.INTERFACE && classOrObjectSymbol.classKind != KtClassKind.ANNOTATION_CLASS)
    }

    internal fun tryGetEffectiveVisibility(symbol: KtCallableSymbol): Visibility? {

        if (symbol !is KtPropertySymbol && symbol !is KtFunctionSymbol) return null

        var visibility = (symbol as? KtSymbolWithVisibility)?.visibility

        analyzeWithSymbolAsContext(symbol) {
            for (overriddenSymbol in symbol.getAllOverriddenSymbols()) {
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

        val modifiers = mutableSetOf(classOrObjectSymbol.toPsiVisibilityForClass(isTopLevel))
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

    private val _extendsList by lazyPub { createInheritanceList(forExtendsList = true, classOrObjectSymbol.superTypes) }
    private val _implementsList by lazyPub { createInheritanceList(forExtendsList = false, classOrObjectSymbol.superTypes) }

    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val declaredMemberScope = classOrObjectSymbol.getDeclaredMemberScope()

            val visibleDeclarations = declaredMemberScope.getCallableSymbols().applyIf(isEnum) {
                filterNot { function ->
                    function is KtFunctionSymbol && function.name.asString().let { it == "values" || it == "valueOf" }
                }
            }.applyIf(classOrObjectSymbol.isObject) {
                filterNot {
                    it is KtKotlinPropertySymbol && it.isConst
                }
            }.applyIf(classOrObjectSymbol.isData) {
                // Technically, synthetic members of `data` class, such as `componentN` or `copy`, are visible.
                // They're just needed to be added later (to be in a backward-compatible order of members).
                filterNot { function ->
                    function is KtFunctionSymbol && function.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED
                }
            }

            val suppressStatic = classOrObjectSymbol.isCompanionObject
            createMethods(visibleDeclarations, result, suppressStaticForMethods = suppressStatic)

            createConstructors(declaredMemberScope.getConstructors(), result)
        }

        addMethodsFromCompanionIfNeeded(result)

        addMethodsFromDataClass(result)
        addDelegatesToInterfaceMethods(result)

        result
    }

    private fun addMethodsFromCompanionIfNeeded(result: MutableList<KtLightMethod>) {
        classOrObjectSymbol.companionObject?.run {
            analyzeWithSymbolAsContext(this) {
                val methods = getDeclaredMemberScope().getCallableSymbols()
                    .filterIsInstance<KtFunctionSymbol>()
                    .filter { it.hasJvmStaticAnnotation() }
                createMethods(methods, result)
            }
        }
    }

    private fun addMethodsFromDataClass(result: MutableList<KtLightMethod>) {
        if (!classOrObjectSymbol.isData) return

        fun createMethodFromAny(ktFunctionSymbol: KtFunctionSymbol) {
            // Similar to `copy`, synthetic members from `Any` should refer to `data` class as origin, not the function in `Any`.
            val lightMemberOrigin = LightMemberOriginForDeclaration(this.kotlinOrigin!!, JvmDeclarationOriginKind.OTHER)
            result.add(
                FirLightSimpleMethodForSymbol(
                    functionSymbol = ktFunctionSymbol,
                    lightMemberOrigin = lightMemberOrigin,
                    containingClass = this,
                    isTopLevel = false,
                    methodIndex = METHOD_INDEX_BASE,
                    suppressStatic = false
                )
            )
        }

        fun KtAnalysisSession.actuallyComesFromAny(functionSymbol: KtFunctionSymbol): Boolean {
            require(functionSymbol.name.isFromAny) {
                "This function's name should one of three Any's function names, but it was ${functionSymbol.name}"
            }

            if (functionSymbol.callableIdIfNonLocal?.classId == StandardClassIds.Any) return true

            return functionSymbol.getAllOverriddenSymbols().any { it.callableIdIfNonLocal?.classId == StandardClassIds.Any }
        }

        // NB: componentN and copy are added during RAW FIR, but synthetic members from `Any` are not.
        // That's why we use declared scope for 'component*' and 'copy', and member scope for 'equals/hashCode/toString'
        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val componentAndCopyFunctions = classOrObjectSymbol.getDeclaredMemberScope()
                .getCallableSymbols { name -> DataClassResolver.isCopy(name) || DataClassResolver.isComponentLike(name) }
                .filter { it.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED }
                .filterIsInstance<KtFunctionSymbol>()

            createMethods(componentAndCopyFunctions, result)

            // Compiler will generate 'equals/hashCode/toString' for data class if they are not final.
            // We want to mimic that.
            val nonFinalFunctionsFromAny = classOrObjectSymbol.getMemberScope()
                .getCallableSymbols { name -> name.isFromAny }
                .filterIsInstance<KtFunctionSymbol>()
                .filterNot { it.modality == Modality.FINAL }
                .filter { actuallyComesFromAny(it) }

            val functionsFromAnyByName = nonFinalFunctionsFromAny.associateBy { it.name }

            // NB: functions from `Any` are not in an alphabetic order.
            functionsFromAnyByName[TO_STRING]?.let { createMethodFromAny(it) }
            functionsFromAnyByName[HASHCODE_NAME]?.let { createMethodFromAny(it) }
            functionsFromAnyByName[EQUALS]?.let { createMethodFromAny(it) }
        }
    }

    private val Name.isFromAny: Boolean
        get() = this == EQUALS || this == HASHCODE_NAME || this == TO_STRING

    private fun addDelegatesToInterfaceMethods(result: MutableList<KtLightMethod>) {

        fun createDelegateMethod(ktFunctionSymbol: KtFunctionSymbol) {
            val kotlinOrigin = ktFunctionSymbol.psi as? KtDeclaration ?: kotlinOrigin!!
            val lightMemberOrigin = LightMemberOriginForDeclaration(kotlinOrigin, JvmDeclarationOriginKind.DELEGATION)
            result.add(
                FirLightSimpleMethodForSymbol(
                    functionSymbol = ktFunctionSymbol,
                    lightMemberOrigin = lightMemberOrigin,
                    containingClass = this,
                    isTopLevel = false,
                    methodIndex = METHOD_INDEX_FOR_NON_ORIGIN_METHOD,
                    suppressStatic = false
                )
            )
        }

        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            classOrObjectSymbol.getDelegatedMemberScope().getCallableSymbols().forEach { functionSymbol ->
                if (functionSymbol is KtFunctionSymbol) {
                    createDelegateMethod(functionSymbol)
                }
            }
        }
    }

    private val _ownFields: List<KtLightField> by lazyPub {

        val result = mutableListOf<KtLightField>()

        // First, add static fields: companion object and fields from companion object
        addCompanionObjectFieldIfNeeded(result)
        addFieldsFromCompanionIfNeeded(result)

        // Then, add instance fields: properties from parameters, and then member properties
        addPropertyBackingFields(result)

        // Next, add INSTANCE field if non-local named object
        addInstanceFieldIfNeeded(result)

        // Last, add fields for enum entries
        addFieldsForEnumEntries(result)

        result
    }

    private fun addFieldsFromCompanionIfNeeded(result: MutableList<KtLightField>) {
        classOrObjectSymbol.companionObject?.run {
            analyzeWithSymbolAsContext(this) {
                getDeclaredMemberScope().getCallableSymbols()
                    .filterIsInstance<KtPropertySymbol>()
                    .filter { it.hasJvmFieldAnnotation() || it.hasJvmStaticAnnotation() || it is KtKotlinPropertySymbol && it.isConst }
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

    private fun addPropertyBackingFields(result: MutableList<KtLightField>) {
        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val propertySymbols = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtPropertySymbol>()
                .applyIf(classOrObjectSymbol.isCompanionObject) {
                    filterNot { it.hasJvmFieldAnnotation() || it is KtKotlinPropertySymbol && it.isConst }
                }
            val propertyGroups = propertySymbols.groupBy { it.isFromPrimaryConstructor }

            val nameGenerator = FirLightField.FieldNameGenerator()

            fun addPropertyBackingField(propertySymbol: KtPropertySymbol) {
                val isJvmField = propertySymbol.hasJvmFieldAnnotation()
                val isJvmStatic = propertySymbol.hasJvmStaticAnnotation()
                val isLateInit = (propertySymbol as? KtKotlinPropertySymbol)?.isLateInit == true

                val forceStatic = classOrObjectSymbol.isObject &&
                        (propertySymbol is KtKotlinPropertySymbol && propertySymbol.isConst || isJvmStatic || isJvmField)
                val takePropertyVisibility = !classOrObjectSymbol.isCompanionObject && (isLateInit || isJvmField || forceStatic)

                createField(
                    declaration = propertySymbol,
                    nameGenerator = nameGenerator,
                    isTopLevel = false,
                    forceStatic = forceStatic,
                    takePropertyVisibility = takePropertyVisibility,
                    result = result
                )
            }

            // First, properties from parameters
            propertyGroups[true]?.forEach(::addPropertyBackingField)
            // Then, regular member properties
            propertyGroups[false]?.forEach(::addPropertyBackingField)
        }
    }

    private fun addInstanceFieldIfNeeded(result: MutableList<KtLightField>) {
        if (classOrObjectSymbol.isNamedObject && !classOrObjectSymbol.isLocal) {
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

    private fun addFieldsForEnumEntries(result: MutableList<KtLightField>) {
        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            if (isEnum) {
                classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                    .filterIsInstance<KtEnumEntrySymbol>()
                    .mapTo(result) { FirLightFieldForEnumEntry(it, this@FirLightClassForSymbol, null) }
            }
        }
    }

    private val KtClassOrObjectSymbol.isObject: Boolean
        get() = classKind == KtClassKind.OBJECT

    private val KtClassOrObjectSymbol.isCompanionObject: Boolean
        get() = classKind == KtClassKind.COMPANION_OBJECT

    private val KtClassOrObjectSymbol.isNamedObject: Boolean
        get() = isObject && !isCompanionObject

    private val KtClassOrObjectSymbol.isLocal: Boolean
        get() = symbolKind == KtSymbolKind.LOCAL

    override fun hashCode(): Int = classOrObjectSymbol.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is FirLightClassForSymbol && classOrObjectSymbol == other.classOrObjectSymbol)

    override fun isInterface(): Boolean = false

    override fun isAnnotationType(): Boolean = false

    override fun isEnum(): Boolean =
        classOrObjectSymbol.classKind == KtClassKind.ENUM_CLASS

    override fun copy(): FirLightClassForSymbol =
        FirLightClassForSymbol(classOrObjectSymbol, manager)

    override fun isValid(): Boolean = super.isValid() && classOrObjectSymbol.isValid()
}
