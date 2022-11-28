/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NON_ORIGIN_METHOD
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUES
import org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUE_OF
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.light.classes.symbol.computeSimpleModality
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForClass
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.utils.addIfNotNull
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

    private val _modifierList: PsiModifierList? by lazyPub {
        val lazyModifiers = lazyPub {
            withClassOrObjectSymbol { classOrObjectSymbol ->
                buildSet {
                    add(classOrObjectSymbol.toPsiVisibilityForClass(isNested = !isTopLevel))
                    addIfNotNull(classOrObjectSymbol.computeSimpleModality())
                    if (!isTopLevel && !classOrObjectSymbol.isInner) {
                        add(PsiModifier.STATIC)
                    }
                }
            }
        }

        val lazyAnnotations = lazyPub {
            withClassOrObjectSymbol { classOrObjectSymbol ->
                classOrObjectSymbol.computeAnnotations(
                    parent = this@SymbolLightClassForClassOrObject,
                    nullability = NullabilityType.Unknown,
                    annotationUseSiteTarget = null,
                )
            }
        }

        SymbolLightClassModifierList(this, lazyModifiers, lazyAnnotations)
    }

    override fun getModifierList(): PsiModifierList? = _modifierList
    override fun getOwnFields(): List<KtLightField> = _ownFields
    override fun getOwnMethods(): List<PsiMethod> = _ownMethods
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

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightMethod>()

            val declaredMemberScope = classOrObjectSymbol.getDeclaredMemberScope()

            val visibleDeclarations = declaredMemberScope.getCallableSymbols().applyIf(isEnum) {
                filterNot { function ->
                    function is KtFunctionSymbol && (function.name == ENUM_VALUES || function.name == ENUM_VALUE_OF)
                }
            }.applyIf(isObject) {
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

            val suppressStatic = isCompanionObject
            createMethods(visibleDeclarations, result, suppressStatic = suppressStatic)

            createConstructors(declaredMemberScope.getConstructors(), result)


            addMethodsFromCompanionIfNeeded(result, classOrObjectSymbol)

            addMethodsFromDataClass(result, classOrObjectSymbol)
            addDelegatesToInterfaceMethods(result, classOrObjectSymbol)

            result
        }
    }

    context(ktAnalysisSession@KtAnalysisSession)
    private fun addMethodsFromDataClass(result: MutableList<KtLightMethod>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        if (!classOrObjectSymbol.isData) return

        fun createMethodFromAny(ktFunctionSymbol: KtFunctionSymbol) {
            // Similar to `copy`, synthetic members from `Any` should refer to `data` class as origin, not the function in `Any`.
            val lightMemberOrigin = classOrObjectDeclaration?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.OTHER) }
            result.add(
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this@ktAnalysisSession,
                    ktFunctionSymbol,
                    lightMemberOrigin,
                    this,
                    METHOD_INDEX_BASE,
                    false,
                    suppressStatic = false,
                )
            )
        }

        fun actuallyComesFromAny(functionSymbol: KtFunctionSymbol): Boolean {
            require(functionSymbol.name.isFromAny) {
                "This function's name should one of three Any's function names, but it was ${functionSymbol.name}"
            }

            if (functionSymbol.callableIdIfNonLocal?.classId == StandardClassIds.Any) return true

            return functionSymbol.getAllOverriddenSymbols().any { it.callableIdIfNonLocal?.classId == StandardClassIds.Any }
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

    private val Name.isFromAny: Boolean
        get() = this == EQUALS || this == HASHCODE_NAME || this == TO_STRING

    context(ktAnalysisSession@KtAnalysisSession)
    private fun addDelegatesToInterfaceMethods(result: MutableList<KtLightMethod>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        fun createDelegateMethod(ktFunctionSymbol: KtFunctionSymbol) {
            val kotlinOrigin = ktFunctionSymbol.psiSafe<KtDeclaration>() ?: classOrObjectDeclaration
            val lightMemberOrigin = kotlinOrigin?.let { LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.DELEGATION) }
            result.add(
                SymbolLightSimpleMethod(
                    ktAnalysisSession = this@ktAnalysisSession,
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

    private val _ownFields: List<KtLightField> by lazyPub {
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
    protected fun addPropertyBackingFields(result: MutableList<KtLightField>, symbolWithMembers: KtSymbolWithMembers) {
        val propertySymbols = symbolWithMembers.getDeclaredMemberScope().getCallableSymbols()
            .filterIsInstance<KtPropertySymbol>()
            .applyIf(isCompanionObject) {
                // All fields for companion object of classes are generated to the containing class
                // For interfaces, only @JvmField-annotated properties are generated to the containing class
                // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
                filter { containingClass?.isInterface == true && !it.hasJvmFieldAnnotation() }
            }

        val propertyGroups = propertySymbols.groupBy { it.isFromPrimaryConstructor }

        val nameGenerator = SymbolLightField.FieldNameGenerator()

        fun addPropertyBackingField(propertySymbol: KtPropertySymbol) {
            val isJvmField = propertySymbol.hasJvmFieldAnnotation()
            val isLateInit = (propertySymbol as? KtKotlinPropertySymbol)?.isLateInit == true
            val isConst = (propertySymbol as? KtKotlinPropertySymbol)?.isConst == true

            val forceStatic = isObject
            val takePropertyVisibility = isLateInit || isJvmField || isConst

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

    context(ktAnalysisSession@KtAnalysisSession)
    private fun addInstanceFieldIfNeeded(result: MutableList<KtLightField>, namedClassOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        if (!isNamedObject || isLocal) return

        result.add(
            SymbolLightFieldForObject(
                ktAnalysisSession = this@ktAnalysisSession,
                objectSymbol = namedClassOrObjectSymbol,
                containingClass = this,
                name = JvmAbi.INSTANCE_FIELD,
                lightMemberOrigin = null,
            )
        )
    }

    context(ktAnalysisSession@KtAnalysisSession)
    private fun addFieldsForEnumEntries(result: MutableList<KtLightField>, classOrObjectSymbol: KtNamedClassOrObjectSymbol) {
        if (!isEnum) return

        classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
            .filterIsInstance<KtEnumEntrySymbol>()
            .mapNotNullTo(result) {
                val enumEntry = it.psiSafe<KtEnumEntry>()
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

    override fun copy(): SymbolLightClassForClassOrObject =
        SymbolLightClassForClassOrObject(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)
}
