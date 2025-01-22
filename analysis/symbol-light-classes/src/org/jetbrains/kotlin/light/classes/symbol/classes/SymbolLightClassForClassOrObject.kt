/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiSymbolPointerCreator
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NON_ORIGIN_METHOD
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor.Companion.createConstructors
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod.Companion.createSimpleMethods
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

    @OptIn(KaImplementationDetail::class)
    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = KaPsiSymbolPointerCreator.symbolPointerOfType(classOrObject),
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
            createInheritanceList(this@SymbolLightClassForClassOrObject, forExtendsList = true, classSymbol.superTypes)
        }
    }

    private val _implementsList by lazyPub {
        withClassSymbol { classSymbol ->
            createInheritanceList(this@SymbolLightClassForClassOrObject, forExtendsList = false, classSymbol.superTypes)
        }
    }

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<PsiMethod>()

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

            val suppressStatic = classKind() == KaClassKind.COMPANION_OBJECT
            createMethods(this@SymbolLightClassForClassOrObject, visibleDeclarations, result, suppressStatic = suppressStatic)
            createConstructors(this@SymbolLightClassForClassOrObject, declaredMemberScope.constructors, result)

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

    private fun KaSession.addMethodsFromDataClass(result: MutableList<PsiMethod>, classSymbol: KaNamedClassSymbol) {
        if (!classSymbol.isData) return

        // NB: componentN and copy are added during RAW FIR, but synthetic members from `Any` are not.
        // That's why we use declared scope for 'component*' and 'copy', and member scope for 'equals/hashCode/toString'
        val componentAndCopyFunctions = classSymbol.declaredMemberScope
            .callables { name -> DataClassResolver.isCopy(name) || DataClassResolver.isComponentLike(name) }
            .filter { it.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED }
            .filterIsInstance<KaNamedFunctionSymbol>()

        createMethods(this@SymbolLightClassForClassOrObject, componentAndCopyFunctions, result)
        generateMethodsFromAny(classSymbol, result)
    }

    private fun KaSession.createMethodFromAny(functionSymbol: KaNamedFunctionSymbol, result: MutableList<PsiMethod>) {
        // Similar to `copy`, synthetic members from `Any` should refer to `data` class as origin, not the function in `Any`.
        val lightMemberOrigin = classOrObjectDeclaration?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.OTHER) }
        createSimpleMethods(
            this@SymbolLightClassForClassOrObject,
            result,
            functionSymbol,
            lightMemberOrigin,
            METHOD_INDEX_BASE,
            isTopLevel = false,
        )
    }

    protected fun KaSession.generateMethodsFromAny(classSymbol: KaNamedClassSymbol, result: MutableList<PsiMethod>) {
        // Compiler will generate 'equals/hashCode/toString' for data/value class if they are not final.
        // We want to mimic that.
        val generatedFunctionsFromAny = classSymbol.memberScope
            .callables(EQUALS, HASHCODE_NAME, TO_STRING)
            .filterIsInstance<KaNamedFunctionSymbol>()
            .filter { it.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED }

        val functionsFromAnyByName = generatedFunctionsFromAny.associateBy { it.name }

        // NB: functions from `Any` are not in an alphabetic order.
        functionsFromAnyByName[TO_STRING]?.let { createMethodFromAny(it, result) }
        functionsFromAnyByName[HASHCODE_NAME]?.let { createMethodFromAny(it, result) }
        functionsFromAnyByName[EQUALS]?.let { createMethodFromAny(it, result) }
    }

    protected fun KaSession.addDelegatesToInterfaceMethods(result: MutableList<PsiMethod>, classSymbol: KaNamedClassSymbol) {
        fun createDelegateMethod(functionSymbol: KaNamedFunctionSymbol) {
            val kotlinOrigin = functionSymbol.psiSafe<KtDeclaration>() ?: classOrObjectDeclaration
            val lightMemberOrigin = kotlinOrigin?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.DELEGATION) }
            createSimpleMethods(
                containingClass = this@SymbolLightClassForClassOrObject,
                result = result,
                functionSymbol = functionSymbol,
                lightMemberOrigin = lightMemberOrigin,
                methodIndex = METHOD_INDEX_FOR_NON_ORIGIN_METHOD,
                isTopLevel = false,
            )
        }

        classSymbol.delegatedMemberScope.callables.forEach { functionSymbol ->
            if (functionSymbol is KaNamedFunctionSymbol) {
                createDelegateMethod(functionSymbol)
            }
        }
    }

    override fun getOwnFields(): List<PsiField> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<PsiField>()

            // First, add static fields: companion object and fields from companion object
            addCompanionObjectFieldIfNeeded(result, classSymbol)
            val nameGenerator = SymbolLightField.FieldNameGenerator()
            addFieldsFromCompanionIfNeeded(result, classSymbol, nameGenerator)

            // Then, add instance fields: properties from parameters, and then member properties
            addPropertyBackingFields(this@SymbolLightClassForClassOrObject, result, classSymbol, nameGenerator)

            // Next, add INSTANCE field if non-local named object
            addInstanceFieldIfNeeded(result, classSymbol)

            // Last, add fields for enum entries
            addFieldsForEnumEntries(result, classSymbol)

            result
        }
    }

    private fun KaSession.addInstanceFieldIfNeeded(result: MutableList<PsiField>, classSymbol: KaNamedClassSymbol) {
        if (classKind() != KaClassKind.OBJECT || isLocal) return

        result.add(
            SymbolLightFieldForObject(
                ktAnalysisSession = this,
                objectSymbol = classSymbol,
                containingClass = this@SymbolLightClassForClassOrObject,
                name = JvmAbi.INSTANCE_FIELD,
                lightMemberOrigin = null,
                isCompanion = false,
            )
        )
    }

    private fun KaSession.addFieldsForEnumEntries(result: MutableList<PsiField>, classSymbol: KaNamedClassSymbol) {
        if (!isEnum) return

        classSymbol.staticDeclaredMemberScope.callables
            .filterIsInstance<KaEnumEntrySymbol>()
            .mapNotNullTo(result) {
                val enumEntry = it.sourcePsiSafe<KtEnumEntry>()
                val name = enumEntry?.name ?: return@mapNotNullTo null
                SymbolLightFieldForEnumEntry(
                    enumEntry = enumEntry,
                    enumEntryName = name,
                    containingClass = this@SymbolLightClassForClassOrObject,
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
