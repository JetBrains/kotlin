/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
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
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
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
        ktAnalysisSession: KaSession,
        ktModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager,
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    ) {
        require(classSymbol.classKind != KaClassKind.INTERFACE && classSymbol.classKind != KaClassKind.ANNOTATION_CLASS)
    }

    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = classOrObject.symbolPointerOfType(),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject !is KtClass || !classOrObject.isInterface() && !classOrObject.isAnnotation())
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(ktModule, classSymbolPointer)
            ),
        )
    }

    override fun getModifierList(): PsiModifierList? = _modifierList
    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    private val _extendsList by lazyPub {
        withClassSymbol { classSymbol ->
            createInheritanceList(forExtendsList = true, classSymbol.superTypes)
        }
    }

    private val _implementsList by lazyPub {
        withClassSymbol { classSymbol ->
            createInheritanceList(forExtendsList = false, classSymbol.superTypes)
        }
    }

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<KtLightMethod>()

            // We should use the combined declared member scope here because an enum class may contain static callables.
            val declaredMemberScope = classSymbol.combinedDeclaredMemberScope

            val visibleDeclarations = declaredMemberScope.callables
                .applyIf(classKind().isObject) {
                    filterNot {
                        it is KaKotlinPropertySymbol && it.isConst
                    }
                }.applyIf(classSymbol.isData) {
                    // Technically, synthetic members of `data` class, such as `componentN` or `copy`, are visible.
                    // They're just needed to be added later (to be in a backward-compatible order of members).
                    filterNot { function ->
                        function is KaNamedFunctionSymbol && function.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED
                    }
                }.applyIf(isEnum && isEnumEntriesDisabled()) {
                    filterNot {
                        it is KaKotlinPropertySymbol && it.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED && it.name == StandardNames.ENUM_ENTRIES
                    }
                }
                .filterNot {
                    it.hasTypeForValueClassInSignature()
                }

            val suppressStatic = classKind() == KaClassKind.COMPANION_OBJECT
            createMethods(visibleDeclarations, result, suppressStatic = suppressStatic)

            createConstructors(declaredMemberScope.constructors, result)


            addMethodsFromCompanionIfNeeded(result, classSymbol)

            addMethodsFromDataClass(result, classSymbol)
            addDelegatesToInterfaceMethods(result, classSymbol)

            result
        }
    }

    private fun isEnumEntriesDisabled(): Boolean {
        return (ktModule as? KaSourceModule)
            ?.languageVersionSettings
            ?.supportsFeature(LanguageFeature.EnumEntries) != true
    }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    private fun addMethodsFromDataClass(result: MutableList<KtLightMethod>, classSymbol: KaNamedClassSymbol) {
        if (!classSymbol.isData) return

        fun createMethodFromAny(functionSymbol: KaNamedFunctionSymbol) {
            // Similar to `copy`, synthetic members from `Any` should refer to `data` class as origin, not the function in `Any`.
            val lightMemberOrigin = classOrObjectDeclaration?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.OTHER) }
            result.add(
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this@KaSession,
                    functionSymbol,
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
        val componentAndCopyFunctions = classSymbol.declaredMemberScope
            .callables { name -> DataClassResolver.isCopy(name) || DataClassResolver.isComponentLike(name) }
            .filter { it.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED }
            .filterIsInstance<KaNamedFunctionSymbol>()

        createMethods(componentAndCopyFunctions, result)

        // Compiler will generate 'equals/hashCode/toString' for data class if they are not final.
        // We want to mimic that.
        val generatedFunctionsFromAny = classSymbol.memberScope
            .callables(EQUALS, HASHCODE_NAME, TO_STRING)
            .filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED }

        val functionsFromAnyByName = generatedFunctionsFromAny.associateBy { it.name }

        // NB: functions from `Any` are not in an alphabetic order.
        functionsFromAnyByName[TO_STRING]?.let { createMethodFromAny(it) }
        functionsFromAnyByName[HASHCODE_NAME]?.let { createMethodFromAny(it) }
        functionsFromAnyByName[EQUALS]?.let { createMethodFromAny(it) }
    }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    protected fun addDelegatesToInterfaceMethods(result: MutableList<KtLightMethod>, classSymbol: KaNamedClassSymbol) {
        fun createDelegateMethod(functionSymbol: KaNamedFunctionSymbol) {
            val kotlinOrigin = functionSymbol.psiSafe<KtDeclaration>() ?: classOrObjectDeclaration
            val lightMemberOrigin = kotlinOrigin?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.DELEGATION) }
            result.add(
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this@KaSession,
                    functionSymbol,
                    lightMemberOrigin,
                    this,
                    METHOD_INDEX_FOR_NON_ORIGIN_METHOD,
                    false,
                    argumentsSkipMask = null,
                    suppressStatic = false,
                )
            )
        }

        classSymbol.delegatedMemberScope.callables.forEach { functionSymbol ->
            if (functionSymbol is KaNamedFunctionSymbol) {
                createDelegateMethod(functionSymbol)
            }
        }
    }

    override fun getOwnFields(): List<KtLightField> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<KtLightField>()

            // First, add static fields: companion object and fields from companion object
            addCompanionObjectFieldIfNeeded(result, classSymbol)
            val nameGenerator = SymbolLightField.FieldNameGenerator()
            addFieldsFromCompanionIfNeeded(result, classSymbol, nameGenerator)

            // Then, add instance fields: properties from parameters, and then member properties
            addPropertyBackingFields(result, classSymbol, nameGenerator)

            // Next, add INSTANCE field if non-local named object
            addInstanceFieldIfNeeded(result, classSymbol)

            // Last, add fields for enum entries
            addFieldsForEnumEntries(result, classSymbol)

            result
        }
    }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    private fun addInstanceFieldIfNeeded(result: MutableList<KtLightField>, classSymbol: KaNamedClassSymbol) {
        if (classKind() != KaClassKind.OBJECT || isLocal) return

        result.add(
            SymbolLightFieldForObject(
                ktAnalysisSession = this@KaSession,
                objectSymbol = classSymbol,
                containingClass = this,
                name = JvmAbi.INSTANCE_FIELD,
                lightMemberOrigin = null,
                isCompanion = false,
            )
        )
    }

    context(KaSession)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    private fun addFieldsForEnumEntries(result: MutableList<KtLightField>, classSymbol: KaNamedClassSymbol) {
        if (!isEnum) return

        classSymbol.staticDeclaredMemberScope.callables
            .filterIsInstance<KaEnumEntrySymbol>()
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
    override fun classKind(): KaClassKind = _classKind

    private val _classKind: KaClassKind by lazyPub {
        when (classOrObjectDeclaration) {
            is KtObjectDeclaration -> {
                if (classOrObjectDeclaration.isCompanion()) KaClassKind.COMPANION_OBJECT else KaClassKind.OBJECT
            }

            is KtClass -> {
                if (classOrObjectDeclaration.isEnum()) KaClassKind.ENUM_CLASS else KaClassKind.CLASS
            }

            else -> withClassSymbol { it.classKind }
        }
    }

    override fun isRecord(): Boolean {
        return _modifierList.hasAnnotation(JvmStandardClassIds.Annotations.JvmRecord.asFqNameString())
    }

    override fun copy(): SymbolLightClassForClassOrObject =
        SymbolLightClassForClassOrObject(classOrObjectDeclaration, classSymbolPointer, ktModule, manager)
}
