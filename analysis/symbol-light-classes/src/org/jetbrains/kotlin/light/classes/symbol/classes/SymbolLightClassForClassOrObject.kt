/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_NON_ORIGIN_METHOD
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.annotations.ExcludeAnnotationFilter
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForObject
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod.Companion.createPropertyAccessors
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor.Companion.createConstructors
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod.Companion.createSimpleMethods
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import kotlin.collections.component1
import kotlin.collections.component2

internal class SymbolLightClassForClassOrObject : SymbolLightClassForNamedClassLike {
    private val isValueClass: Boolean
    override fun isValueClass() = isValueClass

    constructor(
        ktModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager,
    ) : super(
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    ) {
        require(classSymbol.classKind != KaClassKind.INTERFACE && classSymbol.classKind != KaClassKind.ANNOTATION_CLASS)
        isValueClass = classSymbol.isInline
    }

    @OptIn(KaImplementationDetail::class)
    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = classOrObject.createSymbolPointer(ktModule),
        ktModule = ktModule,
        manager = classOrObject.manager,
        isValueClass = classOrObject.hasModifier(KtTokens.VALUE_KEYWORD) || classOrObject.hasModifier(KtTokens.INLINE_KEYWORD),
    ) {
        require(classOrObject !is KtClass || !classOrObject.isInterface() && !classOrObject.isAnnotation())
    }

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
        isValueClass: Boolean,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    ) {
        this.isValueClass = isValueClass
    }

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(ktModule, classSymbolPointer),
                annotationFilter = ExcludeAnnotationFilter.JvmExposeBoxed,
            ),
        )
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList
    override fun getImplementsList(): PsiReferenceList = _implementsList

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
            generateMethodsFromAny(classSymbol, result)

            addDelegatesToInterfaceMethods(result, classSymbol)

            addMethodsFromCollectionsIfNeeded(result, classSymbol)

            result
        }
    }

    private fun KaSession.addMethodsFromCollectionsIfNeeded(result: MutableList<PsiMethod>, classSymbol: KaNamedClassSymbol) {
        if (classSymbol.classKind == KaClassKind.INTERFACE) {
            // TODO?
            return
        }

        val allSupertypes = classSymbol.defaultType.allSupertypes.filterIsInstance<KaClassType>()
        for (supertype in classSymbol.superTypes.filterIsInstance<KaClassType>()) {
            val classId = supertype.classId
            val javaClassId = JavaToKotlinClassMap.mutabilityMappings.find {
                classId == it.kotlinReadOnly || classId == it.kotlinMutable
            }?.javaClass ?: continue

            val kotlinCollectionSymbol = supertype.symbol as? KaClassSymbol ?: continue
            val javaCollectionSymbol = findClass(javaClassId) ?: continue

            val javaBaseClass = javaCollectionSymbol.psi as? PsiClass ?: continue

            val typeParameterMapping = mutableMapOf<PsiTypeParameter, PsiType>()

            javaBaseClass.typeParameters.zip(supertype.typeArguments).forEach { (javaParam, kotlinArg) ->
                val psiType = kotlinArg.type?.asPsiType(useSitePosition = this@SymbolLightClassForClassOrObject, allowErrorTypes = true)
                    ?: return@forEach
                typeParameterMapping[javaParam] = psiType
            }

            val substitutor = PsiSubstitutor.createSubstitutor(typeParameterMapping)

            result += calcMethods(javaBaseClass, javaCollectionSymbol, kotlinCollectionSymbol, substitutor)
        }
    }

    private fun KaSession.calcMethods(
        javaBaseClass: PsiClass,
        javaCollectionSymbol: KaClassSymbol,
        kotlinCollectionSymbol: KaClassSymbol,
        substitutor: PsiSubstitutor,
    ): List<PsiMethod> {

        val kotlinNames = kotlinCollectionSymbol.declaredMemberScope.callables
            .filter { it is KaNamedFunctionSymbol }
            .mapNotNull { it.name }
            .toSet()

        return javaBaseClass.methods.flatMap { method -> methodWrappers(method, javaBaseClass, kotlinNames, substitutor) }
    }

    private val javaGetterNameToKotlinGetterName: Map<String, String> =
        BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.map { (propertyFqName, javaGetterShortName) ->
            Pair(javaGetterShortName.asString(), JvmAbi.getterName(propertyFqName.shortName().asString()))
        }.toMap()

    private val membersWithSpecializedSignature: Set<String> =
        SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SIGNATURES.mapTo(LinkedHashSet()) {
            val fqNameString = it.substringBefore('(').replace('/', '.')
            FqName(fqNameString).shortName().asString()
        }

    // TODO check "go to base method" in IDE
    private fun methodWrappers(
        method: PsiMethod,
        javaBaseClass: PsiClass,
        kotlinNames: Set<Name>,
        substitutor: PsiSubstitutor,
    ): List<PsiMethod> {
        val methodName = method.name

        // TODO map method qualified name to Kotlin's FQN to avoid short name clashes (ex. `size`)
        //  or just remove `StandardNames.FqNames.atomic` names from the original map
        javaGetterNameToKotlinGetterName[methodName]?.let { kotlinName ->
            val finalBridgeForJava = method.finalBridge(substitutor)
            val abstractKotlinGetter = method.wrap(substitutor, name = kotlinName)
            return listOf(finalBridgeForJava, abstractKotlinGetter)
        }

        if (!method.isInKotlinInterface(javaBaseClass, kotlinNames)) {
            // compiler generates stub override
            return listOf(method.openBridge(substitutor))
        }

        return methodsWithSpecializedSignature(method, javaBaseClass, substitutor)
    }

    private fun PsiMethod.isInKotlinInterface(javaBaseClass: PsiClass, kotlinNames: Set<Name>): Boolean {
        if (javaBaseClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP_ENTRY) {
            when (name) {
                "getValue", "getKey" -> return true
            }
        }
        return Name.identifier(name) in kotlinNames
    }

    // TODO recheck toArray
    //  public abstract <T> T[] toArray(T[]);// <T>  toArray(T[])
    //
    // TODO difference with backend in `iterator`
    //  it is de-facto required to implement in `J`, because our backend just throws an Exception (probably a backend bug)
    private fun methodsWithSpecializedSignature(method: PsiMethod, javaBaseClass: PsiClass, substitutor: PsiSubstitutor): List<PsiMethod> {
        val methodName = method.name

        if (methodName !in membersWithSpecializedSignature) return emptyList()

        if (javaBaseClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP) {
            val abstractKotlinVariantWithGeneric = javaUtilMapMethodWithSpecialSignature(method, substitutor) ?: return emptyList()
            val finalBridgeWithObject = method.finalBridge(substitutor)
            return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
        }

        if (methodName in SpecialGenericSignatures.ERASED_COLLECTION_PARAMETER_NAMES) {
            return emptyList()
        }

        if (methodName == "remove" && method.parameterList.parameters.singleOrNull()?.type == PsiTypes.intType()) {
            // remove(int) -> abstract removeAt(int), final bridge remove(int)
            return listOf(method.finalBridge(substitutor), createRemoveAt(method, substitutor))
        }

        if (methodName == "contains") {
            return emptyList()
        }

        val finalBridgeWithObject = method.finalBridge(substitutor)
        val abstractKotlinVariantWithGeneric = method.wrap(substitutor, substituteObjectWith = singleTypeParameterAsType())
        return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
    }

    private fun javaUtilMapMethodWithSpecialSignature(method: PsiMethod, substitutor: PsiSubstitutor): KtLightMethodWrapper? {
        val k = typeParameters[0].asType()
        val v = typeParameters[1].asType()

        val signature = when (method.name) {
            "get" -> MethodSignature(
                parameterTypes = listOf(k),
                returnType = v
            )
            "getOrDefault" -> MethodSignature(
                parameterTypes = listOf(k, v),
                returnType = v
            )
            "containsKey" -> MethodSignature(
                parameterTypes = listOf(k),
                returnType = PsiTypes.booleanType()
            )
            "containsValue" -> MethodSignature(
                parameterTypes = listOf(v),
                returnType = PsiTypes.booleanType()
            )
            "remove" ->
                // only `remove(Object)` pair (i.e. `remove(K)`) is needed
                if (method.parameterList.parametersCount == 1) {
                    MethodSignature(
                        parameterTypes = listOf(k),
                        returnType = v
                    )
                } else {
                    // we don't need `remove(K, V)` as SpecialBridgeMethods#specialMethodsWithDefaults
                    // for `makeDescription(StandardNames.FqNames.mutableMap, "remove", 2)`
                    // it is mapped to SpecialMethodWithDefaultInfo with no `needsGenericSignature`
                    null
                }
            else -> null
        } ?: return null

        return method.wrap(signature = signature, substitutor = substitutor)
    }

    private fun singleTypeParameterAsType(): PsiType = typeParameters.single().asType()

    private fun createRemoveAt(baseMethod: PsiMethod, substitutor: PsiSubstitutor): PsiMethod {
        return baseMethod.wrap(
            name = "removeAt",
            signature = MethodSignature(
                parameterTypes = listOf(PsiTypes.intType()),
                returnType = singleTypeParameterAsType()
            ),
            substitutor = substitutor,
        )
    }

    private fun PsiMethod.finalBridge(substitutor: PsiSubstitutor): KtLightMethodWrapper =
        wrap(substitutor, makeFinal = true, hasImplementation = true)

    private fun PsiMethod.openBridge(substitutor: PsiSubstitutor): KtLightMethodWrapper =
        wrap(substitutor, makeFinal = false, hasImplementation = true)

    private fun PsiMethod.wrap(
        substitutor: PsiSubstitutor,
        makeFinal: Boolean = false,
        hasImplementation: Boolean = false,
        name: String = this.name,
        substituteObjectWith: PsiType? = null,
        signature: MethodSignature? = null,
    ) = KtLightMethodWrapper(
        containingClass = this@SymbolLightClassForClassOrObject,
        baseMethod = this@wrap,
        substitutor = substitutor,
        isFinal = makeFinal,
        name = name,
        substituteObjectWith = substituteObjectWith,
        providedSignature = signature,
        hasImplementation = hasImplementation
    )

    private fun PsiTypeParameter.asType(): PsiType = PsiImmediateClassType(this, PsiSubstitutor.EMPTY)

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
            suppressValueClass = true,
        )
    }

    private fun KaSession.generateMethodsFromAny(classSymbol: KaNamedClassSymbol, result: MutableList<PsiMethod>) {
        if (!classSymbol.isData && !classSymbol.isInline) return

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

    private fun KaSession.addDelegatesToInterfaceMethods(result: MutableList<PsiMethod>, classSymbol: KaNamedClassSymbol) {
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
                suppressValueClass = true,
            )
        }

        classSymbol.delegatedMemberScope.callables.forEach { callableSymbol ->
            when (callableSymbol) {
                is KaNamedFunctionSymbol -> {
                    createDelegateMethod(functionSymbol = callableSymbol)
                }
                is KaKotlinPropertySymbol -> {
                    createPropertyAccessors(
                        lightClass = this@SymbolLightClassForClassOrObject,
                        result = result,
                        declaration = callableSymbol,
                        isTopLevel = false
                    )
                }
                else -> {}
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

    private fun addInstanceFieldIfNeeded(result: MutableList<PsiField>, classSymbol: KaNamedClassSymbol) {
        if (classKind() != KaClassKind.OBJECT || isLocal) return

        result.add(
            SymbolLightFieldForObject(
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
    override fun classKind(): KaClassKind = withClassSymbol { it.classKind }
    override fun isRecord(): Boolean {
        return modifierList.hasAnnotation(JvmStandardClassIds.Annotations.JvmRecord.asFqNameString())
    }

    override fun copy(): SymbolLightClassForClassOrObject = SymbolLightClassForClassOrObject(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager,
        isValueClass = isValueClass,
    )
}

private data class MethodSignature(val parameterTypes: List<PsiType>, val returnType: PsiType)

private class KtLightMethodWrapper(
    private val containingClass: SymbolLightClassForClassOrObject,
    private val baseMethod: PsiMethod,
    private val substitutor: PsiSubstitutor,
    private val name: String,
    private val isFinal: Boolean,
    private val hasImplementation: Boolean,
    private val substituteObjectWith: PsiType?,
    private val providedSignature: MethodSignature?,
) : PsiMethod, KtLightElementBase(containingClass) {

    init {
        if (!hasImplementation && isFinal) {
            error("Can't be final without an implementation")
        }
    }

    private fun substituteType(psiType: PsiType): PsiType {
        val substituted = substitutor.substitute(psiType)
        return if (isJavaLangObject(substituted) && substituteObjectWith != null) {
            substituteObjectWith
        } else {
            substituted
        }
    }

    private fun isJavaLangObject(type: PsiType?): Boolean {
        // TODO or equalsToText?
        return type is PsiClassType && type.canonicalText == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun getPresentation() = baseMethod.presentation

    override val kotlinOrigin get() = null

    override fun hasModifierProperty(name: String) =
        when (name) {
            PsiModifier.DEFAULT -> hasImplementation
            PsiModifier.ABSTRACT -> !hasImplementation
            PsiModifier.FINAL -> isFinal
            else -> baseMethod.hasModifierProperty(name)
        }

    override fun getParameterList(): PsiParameterList {
        return LightParameterListBuilder(manager, KotlinLanguage.INSTANCE).apply {
            baseMethod.parameterList.parameters.forEachIndexed { index, paramFromJava ->
                val type = providedSignature?.parameterTypes?.get(index) ?: substituteType(paramFromJava.type)
                addParameter(
                    LightParameter(
                        paramFromJava.name, type,
                        this@KtLightMethodWrapper, KotlinLanguage.INSTANCE, paramFromJava.isVarArgs
                    )
                )
            }
        }
    }

    override fun getName() = name
    override fun getReturnType() = providedSignature?.returnType ?: baseMethod.returnType?.let { substituteType(it) }

    override fun getTypeParameters() = baseMethod.typeParameters
    override fun getTypeParameterList() = baseMethod.typeParameterList

    override fun findSuperMethods(checkAccess: Boolean) = PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)
    override fun findSuperMethods(parentClass: PsiClass) = PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)
    override fun findSuperMethods() = PsiSuperMethodImplUtil.findSuperMethods(this)
    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean) =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    @Deprecated("Deprecated in Java")
    override fun findDeepestSuperMethod() = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods() = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)
    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)
    override fun getSignature(substitutor: PsiSubstitutor) = MethodSignatureBackedByPsiMethod.create(this, substitutor)
    override fun getReturnTypeElement(): PsiTypeElement? = null
    override fun getContainingClass() = containingClass
    override fun getThrowsList() = baseMethod.throwsList
    override fun hasTypeParameters() = baseMethod.hasTypeParameters()
    override fun isVarArgs() = baseMethod.isVarArgs
    override fun isConstructor() = false
    private val identifier by lazyPub { LightIdentifier(manager, name) }
    override fun getNameIdentifier() = identifier
    override fun getDocComment() = baseMethod.docComment
    override fun getModifierList() = baseMethod.modifierList
    override fun getBody() = null
    override fun isDeprecated() = baseMethod.isDeprecated
    override fun setName(name: String) = cannotModify()

    override fun toString(): String {
        return "$javaClass:$name${parameterList.parameters.map { it.type }.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
    }
}
