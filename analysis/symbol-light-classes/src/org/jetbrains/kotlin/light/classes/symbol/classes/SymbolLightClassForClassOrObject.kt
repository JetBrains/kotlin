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
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
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
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

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

            val allSupertypes = classSymbol.defaultType.allSupertypes
                .filterIsInstance<KaClassType>()
                .filter { it.classId != StandardClassIds.Any }
                .toList()

            val filteredDeclarations = processMemberScopeMappedSpecialSignatures(visibleDeclarations, classSymbol, allSupertypes, result)

            val suppressStatic = classKind() == KaClassKind.COMPANION_OBJECT
            createMethods(this@SymbolLightClassForClassOrObject, filteredDeclarations, result, suppressStatic = suppressStatic)
            createConstructors(this@SymbolLightClassForClassOrObject, declaredMemberScope.constructors, result)

            addMethodsFromCompanionIfNeeded(result, classSymbol)

            addMethodsFromDataClass(result, classSymbol)
            generateMethodsFromAny(classSymbol, result)

            addDelegatesToInterfaceMethods(result, classSymbol, allSupertypes)

            addJavaCollectionMethodStubsIfNeeded(result, classSymbol, allSupertypes)

            result
        }
    }

    private fun KaSession.processMemberScopeMappedSpecialSignatures(
        visibleDeclarations: Sequence<KaCallableSymbol>,
        classSymbol: KaNamedClassSymbol,
        allSupertypes: List<KaClassType>,
        result: MutableList<PsiMethod>
    ): Sequence<KaCallableSymbol> {
        if (allSupertypes.none { it.classId.packageFqName.startsWith(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME) }) {
            // The class definitely doesn't have mapped signatures
            return visibleDeclarations
        }

        val filteredDeclarations = mutableListOf<KaCallableSymbol>()

        for (callableSymbol in visibleDeclarations) {
            when (callableSymbol) {
                is KaNamedFunctionSymbol -> {
                    val overriddenSymbol = findOverriddenCollectionSymbol(callableSymbol)
                    val javaMethod = getJavaMethodForMappedMethodWithSpecialSignature(overriddenSymbol, allSupertypes)
                    if (javaMethod == null) {
                        // Default case: method is not mapped to Java collections method - just create the delegate
                        filteredDeclarations += callableSymbol
                        continue
                    }

                    //val javaReturnType = (javaMethod.returnType as? PsiClassReferenceType)?.resolve()
                    //val defaultType = classSymbol.defaultType
                    //supertype.symbol.typeParameters
                    //val supertypeDefaultType = supertype.symbol?.defaultType
                    //val allOverriddenSymbols = callableSymbol.allOverriddenSymbols

                    // TODO what about indirect inheritance?
                    val supertype = classSymbol.superTypes.firstOrNull() as? KaClassType
                    val typeParameters = javaMethod.containingClass?.typeParameters.orEmpty()
                    val typeArguments = supertype?.typeArguments.orEmpty()
                    val typeParameterMapping = buildMap<PsiTypeParameter, PsiType> {
                        typeParameters.zip(typeArguments).forEach { (typeParameter, typeArgument) ->
                            val type = typeArgument.type?.asPsiType(
                                useSitePosition = this@SymbolLightClassForClassOrObject,
                                allowErrorTypes = true
                            )
                            if (type != null) put(typeParameter, type)
                        }
                    }

                    val substitutor = PsiSubstitutor.createSubstitutor(typeParameterMapping)
                    val isErasedSignature = javaMethod.name in erasedCollectionParameterNames ||
                            // TODO recursively visit the parameter types?
                            callableSymbol.valueParameters.any { it.returnType is KaTypeParameterType }

                    if (isErasedSignature) {
                        result.add(javaMethod.wrap(substitutor, hasImplementation = true, makeFinal = false))
                    } else {
                        filteredDeclarations += callableSymbol
                        result.add(javaMethod.wrap(substitutor, hasImplementation = true, makeFinal = true))
                    }
                }

                else -> {
                    filteredDeclarations += callableSymbol
                }
            }
        }

        return filteredDeclarations.asSequence()
    }

    private fun KaSession.addJavaCollectionMethodStubsIfNeeded(
        result: MutableList<PsiMethod>,
        classSymbol: KaNamedClassSymbol,
        allSupertypes: List<KaClassType>
    ) {
        if (classSymbol.classKind != KaClassKind.CLASS) {
            return
        }

        if (allSupertypes.any { (it.symbol as? KaClassSymbol)?.classKind != KaClassKind.INTERFACE }) {
            // Collection method stubs should be created only inside the first non-interface subtype of the Kotlin mapped class
            return
        }

        val closestMappedSupertype = allSupertypes.find { mapKotlinClassToJava(it.classId) != null } ?: return
        val javaClassId = mapKotlinClassToJava(closestMappedSupertype.classId) ?: return
        val kotlinCollectionSymbol = closestMappedSupertype.symbol as? KaClassSymbol ?: return
        val javaCollectionSymbol = findClass(javaClassId) ?: return
        val javaCollectionPsiClass = javaCollectionSymbol.psi as? PsiClass ?: return

        val typeParameterMapping = buildMap<PsiTypeParameter, PsiType> {
            javaCollectionPsiClass.typeParameters.zip(closestMappedSupertype.typeArguments).forEach { (javaParam, kotlinArg) ->
                val psiType = kotlinArg.type?.asPsiType(useSitePosition = this@SymbolLightClassForClassOrObject, allowErrorTypes = true)
                    ?: return@forEach
                put(javaParam, psiType)
            }
        }

        val substitutor = PsiSubstitutor.createSubstitutor(typeParameterMapping)
        calcMethods(javaCollectionPsiClass, kotlinCollectionSymbol, substitutor, result)
    }

    private fun mapKotlinClassToJava(classId: ClassId): ClassId? {
        return JavaToKotlinClassMap.mutabilityMappings.find {
            classId == it.kotlinReadOnly || classId == it.kotlinMutable
        }?.javaClass
    }

    private fun KaSession.calcMethods(
        javaCollectionPsiClass: PsiClass,
        kotlinCollectionSymbol: KaClassSymbol,
        substitutor: PsiSubstitutor,
        result: MutableList<PsiMethod>,
    ) {
        val kotlinNames = kotlinCollectionSymbol.memberScope.callables
            .filter { it is KaNamedFunctionSymbol }
            // default methods in Java collection (ex. toArray() overload from Java 11, which blocks generation of 2 other overloads)
            .filter { it.origin != KaSymbolOrigin.JAVA_SOURCE && it.origin != KaSymbolOrigin.JAVA_LIBRARY }
            .mapNotNull { it.name }
            .toSet()

        val javaMethods = javaCollectionPsiClass.methods
            // seems like there is no need to override default methods
            .filterNot { it.hasModifierProperty(PsiModifier.DEFAULT) }

        val candidateMethods = javaMethods.flatMap { method -> methodWrappers(method, javaCollectionPsiClass, kotlinNames, substitutor) }

        // TODO why PsiSubstitutor.EMPTY?
        val existingSignatures = result.map { it.getSignature(PsiSubstitutor.EMPTY) }.toSet()
        result += candidateMethods.filter { candidateMethod ->
            candidateMethod.getSignature(PsiSubstitutor.EMPTY) !in existingSignatures
        }
    }

    private val javaGetterNameToKotlinGetterName: Map<String, String> = buildMap {
        BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.forEach { (propertyFqName, javaGetterShortName) ->
            put(javaGetterShortName.asString(), JvmAbi.getterName(propertyFqName.shortName().asString()))
        }
    }

    private val membersWithSpecializedSignature: Set<String> = buildSet {
        addAll(SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SHORT_NAMES.map { it.asString() })
        add("addAll")
        add("putAll")
    }

    private val erasedCollectionParameterNames: Set<String> = buildSet {
        addAll(SpecialGenericSignatures.ERASED_COLLECTION_PARAMETER_NAMES)
        add("addAll")
        add("putAll")
    }

    // TODO check "go to base method", "go to declaration" in IDE
    private fun methodWrappers(
        method: PsiMethod,
        javaCollectionPsiClass: PsiClass,
        kotlinNames: Set<Name>,
        substitutor: PsiSubstitutor,
    ): List<PsiMethod> {
        val methodName = method.name

        // TODO map method qualified name to Kotlin's FQN to avoid short name clashes (ex. `size`)
        //  or just remove `StandardNames.FqNames.atomic` names from the original map
        javaGetterNameToKotlinGetterName[methodName]?.let { kotlinName ->
            // TODO workaround for a (FIR?) bug
            //listAbstractClass.kt:
            //  abstract class CList2<Elem> : List<Elem> by emptyList<Elem>()
            //  delegated property "size" has a getter -> we generate it normally as "getSize"
            //
            //mutableListClass.kt:
            //  abstract class CMutableList2<Elem> : MutableList<Elem> by mutableListOf<Elem>()
            //  delegated property "size" has NO getter -> we try to generate it as "abstract" from `methodWrappers` -> red code
            val hasImplementation = methodName == "size" && withClassSymbol { classSymbol ->
                classSymbol.delegatedMemberScope.callables(Name.identifier("size")).toList().isNotEmpty()
            }

            val finalBridgeForJava = method.finalBridge(substitutor)
            val abstractKotlinGetter = method.wrap(substitutor, name = kotlinName, hasImplementation = hasImplementation)
            return listOf(finalBridgeForJava, abstractKotlinGetter)
        }

        // TODO filter out all overrides?
        if (methodName == "equals" || methodName == "hashCode" || methodName == "toString") {
            return emptyList()
        }

        if (!method.isInKotlinInterface(javaCollectionPsiClass, kotlinNames)) {
            // compiler generates stub override
            return listOf(method.openBridge(substitutor))
        }

        // TODO
        return methodsWithSpecializedSignature(method, javaCollectionPsiClass, substitutor)
    }

    private fun PsiMethod.isInKotlinInterface(javaCollectionPsiClass: PsiClass, kotlinNames: Set<Name>): Boolean {
        if (javaCollectionPsiClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP_ENTRY) {
            when (name) {
                "getValue", "getKey" -> return true
            }
        }
        return Name.identifier(name) in kotlinNames
    }

    private fun methodsWithSpecializedSignature(
        method: PsiMethod,
        javaCollectionPsiClass: PsiClass,
        substitutor: PsiSubstitutor,
    ): List<PsiMethod> {
        val methodName = method.name
        if (!mayHaveSpecializedSignature(methodName)) {
            return emptyList()
        }

        if (javaCollectionPsiClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP) {
            val abstractKotlinVariantWithGeneric = javaUtilMapMethodWithSpecialSignature(method, substitutor) ?: return emptyList()
            val finalBridgeWithObject = method.finalBridge(substitutor)
            return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
        }

        if (methodName == "remove") {
            if (method.parameterList.parameters.singleOrNull()?.type == PsiTypes.intType()) {
                // remove(int) -> abstract removeAt(int), final bridge remove(int)
                return listOf(method.finalBridge(substitutor), createRemoveAt(method, substitutor))
            } else if (javaCollectionPsiClass.qualifiedName == CommonClassNames.JAVA_UTIL_ITERATOR) {
                // java.util.Iterator#remove() is a default method and should have been filtered out, but isn't for some reason
                return emptyList()
            }
        }

        val type = substitutor.substitutionMap.values.single()
        if (type.isTypeParameter()) return emptyList()

        val finalBridgeWithObject = method.finalBridge(substitutor)
        val abstractKotlinVariantWithGeneric = method.wrap(substitutor, substituteObjectWith = type)
        return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
    }

    // TODO clarify semantics, currently looks suspicious
    private fun mayHaveSpecializedSignature(methodName: String): Boolean =
        methodName !in erasedCollectionParameterNames && methodName in membersWithSpecializedSignature

    private fun PsiType.isTypeParameter(): Boolean =
        this is PsiClassType && this.resolve() is PsiTypeParameter

    private fun javaUtilMapMethodWithSpecialSignature(method: PsiMethod, substitutor: PsiSubstitutor): KtLightMethodWrapper? {
        val typeParameters = substitutor.substitutionMap.keys
        val kOriginal = substitutor.substitutionMap[typeParameters.find { it.name == "K" }] ?: return null
        val vOriginal = substitutor.substitutionMap[typeParameters.find { it.name == "V" }] ?: return null

        // Perform erasure: map all type parameters of k and v to java.lang.Object
//        val erasureMapping = mutableMapOf<PsiTypeParameter, PsiType>()
//        collectTypeParameters(kOriginal, erasureMapping)
//        collectTypeParameters(vOriginal, erasureMapping)

//        val erasureSubstitutor = PsiSubstitutor.createSubstitutor(erasureMapping)
        val k = substitutor.substitute(kOriginal) ?: kOriginal
        val v = substitutor.substitute(vOriginal) ?: vOriginal

        val signature = when (method.name) {
            "get" -> {
                if (k.isTypeParameter()) return null

                MethodSignature(
                    parameterTypes = listOf(k),
                    returnType = v
                )
            }

            // TODO is it needed?
/*            "getOrDefault" -> MethodSignature(
                parameterTypes = listOf(k, v),
                returnType = v
            )*/

            "containsKey" -> {
                if (k.isTypeParameter()) return null

                MethodSignature(
                    parameterTypes = listOf(k),
                    returnType = PsiTypes.booleanType()
                )
            }

            "containsValue" -> {
                if (v.isTypeParameter()) return null

                MethodSignature(
                    parameterTypes = listOf(v),
                    returnType = PsiTypes.booleanType()
                )
            }

            "remove" -> {
                if (k.isTypeParameter()) return null

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
            }
            else -> null
        } ?: return null

        return method.wrap(signature = signature, substitutor = substitutor)
    }

    private fun createRemoveAt(baseMethod: PsiMethod, substitutor: PsiSubstitutor): PsiMethod {
        return baseMethod.wrap(
            name = "removeAt",
//            signature = MethodSignature(
//                parameterTypes = listOf(PsiTypes.intType()),
//                returnType = singleTypeParameterAsType()
//            ),
            substitutor = substitutor,
        )
    }

    private fun PsiMethod.finalBridge(substitutor: PsiSubstitutor): KtLightMethodWrapper =
        wrap(substitutor, makeFinal = true, hasImplementation = true)

    private fun PsiMethod.openBridge(substitutor: PsiSubstitutor): KtLightMethodWrapper =
        wrap(substitutor, makeFinal = false, hasImplementation = true)

    private fun PsiMethod.wrap(
        substitutor: PsiSubstitutor? = null,
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

    private fun KaSession.addDelegatesToInterfaceMethods(
        result: MutableList<PsiMethod>,
        classSymbol: KaNamedClassSymbol,
        allSupertypes: List<KaClassType>
    ) {
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
                    val original = callableSymbol.fakeOverrideOriginal as KaNamedFunctionSymbol // TODO remove cast
                    val javaMethod = getJavaMethodForMappedMethodWithSpecialSignature(original, allSupertypes)
                    if (javaMethod == null) {
                        // Default case: method is not mapped to Java collections method - just create the delegate
                        createDelegateMethod(functionSymbol = callableSymbol)
                        return@forEach
                    }

                    //val javaReturnType = (javaMethod.returnType as? PsiClassReferenceType)?.resolve()
                    //val defaultType = classSymbol.defaultType
                    //supertype.symbol.typeParameters
                    //val supertypeDefaultType = supertype.symbol?.defaultType
                    //val allOverriddenSymbols = callableSymbol.allOverriddenSymbols

                    // TODO what about indirect inheritance?
                    val supertype = classSymbol.superTypes.firstOrNull() as? KaClassType
                    val typeParameters = javaMethod.containingClass?.typeParameters.orEmpty()
                    val typeArguments = supertype?.typeArguments.orEmpty()
                    val typeParameterMapping = buildMap<PsiTypeParameter, PsiType> {
                        typeParameters.zip(typeArguments).forEach { (typeParameter, typeArgument) ->
                            val type = typeArgument.type?.asPsiType(
                                useSitePosition = this@SymbolLightClassForClassOrObject,
                                allowErrorTypes = true
                            )
                            if (type != null) put(typeParameter, type)
                        }
                    }

                    val substitutor = PsiSubstitutor.createSubstitutor(typeParameterMapping)
                    val isErasedSignature = javaMethod.name in erasedCollectionParameterNames ||
                            // TODO recursively visit the parameter types?
                            callableSymbol.valueParameters.any { it.returnType is KaTypeParameterType }

                    if (isErasedSignature) {
                        result.add(javaMethod.wrap(substitutor, hasImplementation = true, makeFinal = false))
                    } else {
                        createDelegateMethod(functionSymbol = callableSymbol)
                        result.add(javaMethod.wrap(substitutor, hasImplementation = true, makeFinal = true))
                    }
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

    private fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)

    private fun KaSession.getJavaMethodForMappedMethodWithSpecialSignature(
        symbol: KaNamedFunctionSymbol?,
        allSupertypes: List<KaClassType>,
    ): PsiMethod? {
        if (symbol == null) return null
        if (!symbol.isFromKotlinCollections()) return null
        val symbolName = symbol.name.asString()

        return when {
            matchesMapRemoveMethod(symbol) ->
                getJavaMapClass(allSupertypes)?.methods?.find { it.name == "remove" }
            matchesAddAllMethod(symbol) ->
                getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == "addAll" }
            matchesAddAll2Method(symbol) ->
                getJavaListClass(allSupertypes)?.methods?.find { it.name == "addAll" && it.parameters.size == 2 }
            symbolName == "contains" ->
                getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == "contains" }
            symbolName == "remove" ->
                getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == "remove" }
            symbolName == "containsAll" ->
                getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == "containsAll" }
            symbolName == "removeAll" ->
                getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == "removeAll" }
            symbolName == "retainAll" ->
                getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == "retainAll" }
            symbolName == "indexOf" ->
                getJavaListClass(allSupertypes)?.methods?.find { it.name == "indexOf" }
            symbolName == "lastIndexOf" ->
                getJavaListClass(allSupertypes)?.methods?.find { it.name == "lastIndexOf" }
            symbolName == "containsKey" ->
                getJavaMapClass(allSupertypes)?.methods?.find { it.name == "containsKey" }
            symbolName == "containsValue" ->
                getJavaMapClass(allSupertypes)?.methods?.find { it.name == "containsValue" }
            symbolName == "get" ->
                getJavaMapClass(allSupertypes)?.methods?.find { it.name == "get" }
            symbolName == "putAll" ->
                getJavaMapClass(allSupertypes)?.methods?.find { it.name == "putAll" }
            else -> null
        }
    }

    private fun KaSession.getJavaCollectionClass(allSupertypes: List<KaClassType>): PsiClass? =
        getJavaClass(allSupertypes, kotlinClassId = StandardClassIds.Collection)

    private fun KaSession.getJavaListClass(allSupertypes: List<KaClassType>): PsiClass? =
        getJavaClass(allSupertypes, kotlinClassId = StandardClassIds.List)

    private fun KaSession.getJavaMapClass(allSupertypes: List<KaClassType>): PsiClass? =
        getJavaClass(allSupertypes, kotlinClassId = StandardClassIds.Map)

    private fun KaSession.getJavaClass(allSupertypes: List<KaClassType>, kotlinClassId: ClassId): PsiClass? {
        val kotlinCollection = allSupertypes.find { it.classId == kotlinClassId } ?: return null
        val javaClassId = mapKotlinClassToJava(kotlinCollection.classId) ?: return null
        val javaCollectionSymbol = findClass(javaClassId) ?: return null
        return javaCollectionSymbol.psi as? PsiClass
    }

    private fun KaCallableSymbol.isFromKotlinCollections(): Boolean =
        callableId.packageName.startsWith(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME)

    private fun KaSession.findOverriddenCollectionSymbol(symbol: KaNamedFunctionSymbol): KaNamedFunctionSymbol? {
        return symbol.allOverriddenSymbols.find { it.isFromKotlinCollections() } as? KaNamedFunctionSymbol
    }

    private fun matchesAddAllMethod(symbol: KaNamedFunctionSymbol): Boolean {
        return symbol.name.asString() == "addAll" && symbol.valueParameters.size == 1
    }

    private fun matchesAddAll2Method(symbol: KaNamedFunctionSymbol): Boolean {
        return symbol.callableId?.classId == StandardClassIds.MutableList && symbol.name.asString() == "addAll" && symbol.valueParameters.size == 2
    }

    private fun matchesMapRemoveMethod(symbol: KaNamedFunctionSymbol): Boolean {
        return symbol.callableId?.classId == StandardClassIds.MutableMap && symbol.name.asString() == "remove"
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
    private val substitutor: PsiSubstitutor?,
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
        val substituted = substitutor?.substitute(psiType) ?: psiType
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
            // TODO PsiClassRenderer renders `abstract` everywhere
//            PsiModifier.DEFAULT -> hasImplementation
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KtLightMethodWrapper) return false

        if (name != other.name) return false
        if (isFinal != other.isFinal) return false
        if (hasImplementation != other.hasImplementation) return false
        if (providedSignature != other.providedSignature) return false
        if (returnType != other.returnType) return false
        if (substitutor != other.substitutor) return false
        if (substituteObjectWith != other.substituteObjectWith) return false
        if (baseMethod != other.baseMethod) return false
        if (containingClass != other.containingClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isFinal.hashCode()
        result = 31 * result + hasImplementation.hashCode()
        result = 31 * result + (providedSignature?.hashCode() ?: 0)
        result = 31 * result + (returnType?.hashCode() ?: 0)
        result = 31 * result + substitutor.hashCode()
        result = 31 * result + (substituteObjectWith?.hashCode() ?: 0)
        result = 31 * result + baseMethod.hashCode()
        result = 31 * result + containingClass.hashCode()
        return result
    }

    // TODO remove PSI access
    override fun toString(): String {
        return "$javaClass:$name${parameterList.parameters.map { it.type }.joinToString(prefix = "(", postfix = ")", separator = ", ")}"
    }
}
