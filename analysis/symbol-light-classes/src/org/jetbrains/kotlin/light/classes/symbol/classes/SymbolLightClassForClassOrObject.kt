/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NON_ORIGIN_METHOD
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal open class SymbolLightClassForClassOrObject : SymbolLightClassForNamedClassLike {
    constructor(
        ktAnalysisSession: KtAnalysisSession,
        ktModule: KtModule,
        classOrObjectSymbol: KtNamedClassOrObjectSymbol,
        manager: PsiManager,
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = classOrObjectSymbol,
        manager = manager,
    ) {
        require(classOrObjectSymbol.classKind != KtClassKind.INTERFACE && classOrObjectSymbol.classKind != KtClassKind.ANNOTATION_CLASS)
    }

    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KtModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classOrObjectSymbolPointer = classOrObject.symbolPointerOfType(),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject !is KtClass || !classOrObject.isInterface() && !classOrObject.isAnnotation())
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classOrObjectSymbolPointer: KtSymbolPointer<KtNamedClassOrObjectSymbol>,
        ktModule: KtModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classOrObjectSymbolPointer = classOrObjectSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(ktModule, classOrObjectSymbolPointer)
            ),
        )
    }

    override fun getModifierList(): PsiModifierList? = _modifierList
    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    private val _extendsList by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            createInheritanceList(forExtendsList = true, classOrObjectSymbol.superTypes)
        }
    }

    private val _implementsList by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            createInheritanceList(forExtendsList = false, classOrObjectSymbol.superTypes)
        }
    }

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightMethod>()

            // We should use the combined declared member scope here because an enum class may contain static callables.
            val declaredMemberScope = classOrObjectSymbol.getCombinedDeclaredMemberScope()

            val visibleDeclarations = declaredMemberScope.getCallableSymbols()
                .applyIf(classKind().isObject) {
                    filterNot {
                        it is KtKotlinPropertySymbol && it.isConst
                    }
                }.applyIf(classOrObjectSymbol.isData) {
                    // Technically, synthetic members of `data` class, such as `componentN` or `copy`, are visible.
                    // They're just needed to be added later (to be in a backward-compatible order of members).
                    filterNot { function ->
                        function is KtFunctionSymbol && function.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED
                    }
                }.applyIf(isEnum && isEnumEntriesDisabled()) {
                    filterNot {
                        it is KtKotlinPropertySymbol && it.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED && it.name == StandardNames.ENUM_ENTRIES
                    }
                }
                .filterNot {
                    it.hasTypeForValueClassInSignature()
                }

            val suppressStatic = classKind() == KtClassKind.COMPANION_OBJECT
            createMethods(visibleDeclarations, result, suppressStatic = suppressStatic)

            createConstructors(declaredMemberScope.getConstructors(), result)


            addMethodsFromCompanionIfNeeded(result, classOrObjectSymbol)

            addMethodsFromDataClass(result, classOrObjectSymbol)
            addDelegatesToInterfaceMethods(result, classOrObjectSymbol)

            result
        }
    }

    private fun isEnumEntriesDisabled(): Boolean {
        return (ktModule as? KtSourceModule)
            ?.languageVersionSettings
            ?.supportsFeature(LanguageFeature.EnumEntries) != true
    }

    context(KtAnalysisSession)
    private fun addMethodsFromDataClass(result: MutableList<KtLightMethod>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        if (!classOrObjectSymbol.isData) return

        fun createMethodFromAny(ktFunctionSymbol: KtFunctionSymbol) {
            // Similar to `copy`, synthetic members from `Any` should refer to `data` class as origin, not the function in `Any`.
            val lightMemberOrigin = classOrObjectDeclaration?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.OTHER) }
            result.add(
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this@KtAnalysisSession,
                    ktFunctionSymbol,
                    lightMemberOrigin,
                    this,
                    METHOD_INDEX_BASE,
                    false,
                    suppressStatic = false,
                )
            )
        }

        // NB: componentN and copy are added during RAW FIR, but synthetic members from `Any` are not.
        // That's why we use declared scope for 'component*' and 'copy', and member scope for 'equals/hashCode/toString'
        val componentAndCopyFunctions = classOrObjectSymbol.getDeclaredMemberScope()
            .getCallableSymbols { name -> DataClassResolver.isCopy(name) || DataClassResolver.isComponentLike(name) }
            .filter { it.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED }
            .filterIsInstance<KtFunctionSymbol>()

        createMethods(componentAndCopyFunctions, result)

        // Compiler will generate 'equals/hashCode/toString' for data class if they are not final.
        // We want to mimic that.
        val generatedFunctionsFromAny = classOrObjectSymbol.getMemberScope()
            .getCallableSymbols(EQUALS, HASHCODE_NAME, TO_STRING)
            .filterIsInstance<KtFunctionSymbol>()
            .filter { it.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED }

        val functionsFromAnyByName = generatedFunctionsFromAny.associateBy { it.name }

        // NB: functions from `Any` are not in an alphabetic order.
        functionsFromAnyByName[TO_STRING]?.let { createMethodFromAny(it) }
        functionsFromAnyByName[HASHCODE_NAME]?.let { createMethodFromAny(it) }
        functionsFromAnyByName[EQUALS]?.let { createMethodFromAny(it) }
    }

    context(KtAnalysisSession)
    private fun addDelegatesToInterfaceMethods(result: MutableList<KtLightMethod>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        fun createDelegateMethod(ktFunctionSymbol: KtFunctionSymbol) {
            val kotlinOrigin = ktFunctionSymbol.psiSafe<KtDeclaration>() ?: classOrObjectDeclaration
            val lightMemberOrigin = kotlinOrigin?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.DELEGATION) }
            result.add(
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this@KtAnalysisSession,
                    ktFunctionSymbol,
                    lightMemberOrigin,
                    this,
                    METHOD_INDEX_FOR_NON_ORIGIN_METHOD,
                    false,
                    argumentsSkipMask = null,
                    suppressStatic = false,
                )
            )
        }

        classOrObjectSymbol.getDelegatedMemberScope().getCallableSymbols().forEach { functionSymbol ->
            if (functionSymbol is KtFunctionSymbol) {
                createDelegateMethod(functionSymbol)
            }
        }
    }

    override fun getOwnFields(): List<KtLightField> = cachedValue {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightField>()

            // First, add static fields: companion object and fields from companion object
            addCompanionObjectFieldIfNeeded(result, classOrObjectSymbol)
            addFieldsFromCompanionIfNeeded(result, classOrObjectSymbol)

            // Then, add instance fields: properties from parameters, and then member properties
            addPropertyBackingFields(result, classOrObjectSymbol)

            // Next, add INSTANCE field if non-local named object
            addInstanceFieldIfNeeded(result, classOrObjectSymbol)

            // Last, add fields for enum entries
            addFieldsForEnumEntries(result, classOrObjectSymbol)

            result
        }
    }

    context(KtAnalysisSession)
    private fun addInstanceFieldIfNeeded(result: MutableList<KtLightField>, namedClassOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        if (classKind() != KtClassKind.OBJECT || isLocal) return

        result.add(
            SymbolLightFieldForObject(
                ktAnalysisSession = this@KtAnalysisSession,
                objectSymbol = namedClassOrObjectSymbol,
                containingClass = this,
                name = JvmAbi.INSTANCE_FIELD,
                lightMemberOrigin = null,
                isCompanion = false,
            )
        )
    }

    context(KtAnalysisSession)
    private fun addFieldsForEnumEntries(result: MutableList<KtLightField>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        if (!isEnum) return

        classOrObjectSymbol.getStaticDeclaredMemberScope().getCallableSymbols()
            .filterIsInstance<KtEnumEntrySymbol>()
            .mapNotNullTo(result) {
                val enumEntry = it.sourcePsiSafe<KtEnumEntry>()
                val name = enumEntry?.name ?: return@mapNotNullTo null
                SymbolLightFieldForEnumEntry(
                    enumEntry = enumEntry,
                    enumEntryName = name,
                    containingClass = this,
                )
            }
    }

    override fun isInterface(): Boolean = false
    override fun isAnnotationType(): Boolean = false
    override fun classKind(): KtClassKind = _classKind

    private val _classKind: KtClassKind by lazyPub {
        when (classOrObjectDeclaration) {
            is KtObjectDeclaration -> {
                if (classOrObjectDeclaration.isCompanion()) KtClassKind.COMPANION_OBJECT else KtClassKind.OBJECT
            }

            is KtClass -> {
                if (classOrObjectDeclaration.isEnum()) KtClassKind.ENUM_CLASS else KtClassKind.CLASS
            }

            else -> withClassOrObjectSymbol { it.classKind }
        }
    }

    override fun isRecord(): Boolean {
        return _modifierList.hasAnnotation(JvmStandardClassIds.Annotations.JvmRecord.asFqNameString())
    }

    override fun copy(): SymbolLightClassForClassOrObject =
        SymbolLightClassForClassOrObject(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)
}
