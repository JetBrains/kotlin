/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.light.classes.symbol.methods.MethodSignature
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedClass
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

/**
 * Java getters with non-standard names that have corresponding Kotlin properties with different JVM ABI names.
 *
 * For example, Java `size()` method corresponds to Kotlin `size` property, that is translated to `getSize()` method.
 * In this case, the light class will have both `size()` and `getSize()` methods (with different modalities).
 */
private val JAVA_GETTER_NAME_TO_KOTLIN_GETTER_NAME_WITH_DIFFERENT_ABI: Map<String, String> = buildMap {
    BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.forEach { (propertyFqName, javaGetterShortName) ->
        put(javaGetterShortName.asString(), JvmAbi.getterName(propertyFqName.shortName().asString()))
    }
}

/**
 * Java map entry methods that have corresponding Kotlin properties with the same JVM ABI name (`key` -> `getKey`, `value` -> `getValue`).
 */
private val JAVA_MAP_ENTRY_METHODS: Set<String> = buildSet {
    add("getKey")
    add("getValue")
}

private val MEMBERS_WITH_SPECIAL_SIGNATURE: Set<String> = buildSet {
    addAll(SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SHORT_NAMES.map { it.asString() })
    addAll(JAVA_MAP_ENTRY_METHODS)
}

private val ERASED_COLLECTION_METHOD_NAMES: Set<String> = buildSet {
    addAll(SpecialGenericSignatures.ERASED_COLLECTION_PARAMETER_NAMES)
    add("addAll")
    add("putAll")
}

/**
 * This function is responsible for filtering and transforming callable declarations when the containing
 * class extends or implements Kotlin collection types (from `kotlin.collections` package) that map to
 * Java collection interfaces. It ensures that the generated light class methods correctly bridge between
 * Kotlin's collection API and Java's collection API, handling type erasure and special signatures.
 *
 * @param containingClass The light class being constructed
 * @param callableDeclarations All callable symbols (functions, properties) declared in the class
 * @param allSupertypes All supertypes of the containing class
 * @param result Mutable list where generated bridge methods are added
 * @return Filtered sequence of callable symbols that should be processed normally by the caller.
 *         Functions that were fully replaced by bridge methods are excluded from this sequence.
 *
 * @see processPossiblyMappedMethod
 */
internal fun KaSession.processOwnDeclarationsMappedSpecialSignaturesAware(
    containingClass: SymbolLightClassForClassOrObject,
    callableDeclarations: Sequence<KaCallableSymbol>,
    allSupertypes: List<KaClassType>,
    result: MutableList<PsiMethod>,
): Sequence<KaCallableSymbol> {
    if (!hasCollectionSupertype(allSupertypes)) {
        return callableDeclarations
    }

    val filteredDeclarations = buildList {
        for (callableSymbol in callableDeclarations) {
            if (callableSymbol is KaNamedFunctionSymbol) {
                val kotlinCollectionFunction = findOverriddenCollectionSymbol(callableSymbol)
                if (kotlinCollectionFunction == null) {
                    add(callableSymbol)
                    continue
                }

                val shouldCreateRegularDeclaration = processPossiblyMappedMethod(
                    containingClass = containingClass,
                    ownFunction = callableSymbol,
                    kotlinCollectionFunction = kotlinCollectionFunction,
                    allSupertypes = allSupertypes,
                    result = result,
                    originKind = JvmDeclarationOriginKind.OTHER
                )
                if (shouldCreateRegularDeclaration) {
                    add(callableSymbol)
                }
            } else {
                add(callableSymbol)
            }
        }
    }

    return filteredDeclarations.asSequence()
}

internal fun hasCollectionSupertype(allSupertypes: List<KaClassType>): Boolean =
    allSupertypes.any { it.classId.packageFqName.startsWith(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME) }

/**
 * Processes a Kotlin function that may be mapped to a Java collection method with special signature requirements.
 *
 * This method handles the complex interplay between Kotlin collection methods and their Java counterparts,
 * particularly for methods like `contains`, `remove`, `get`, etc., which require special handling due to
 * type erasure and signature differences between Kotlin and Java collections.
 *
 * When a Kotlin function overrides or delegates to a function from a `kotlin.collections` type that maps
 * to a Java collection method (e.g., `java.util.Collection`, `java.util.List`, `java.util.Map`), this
 * method determines how to generate the appropriate PSI methods:
 *
 * 1. **Non-mapped case** (returns `true`):
 *    - The method is not mapped to a special Java collection method
 *    - No bridge methods are generated
 *    - The original Kotlin method should be generated normally
 *
 * 2. **Special signature case** (returns `true`):
 *    - The method has special generic types (e.g., `remove(K)` where K is String)
 *    - Generates a final bridge method with the special Java signature
 *    - The original Kotlin method SHOULD still be generated with its Kotlin signature
 *
 * 3. **Erased signature case** (returns `false`):
 *    - The method has type parameters that get erased (e.g., `contains(Object)` in Java)
 *    - Generates a single non-final bridge method with the erased Java signature
 *    - The original Kotlin method should NOT be generated separately
 *
 * @param containingClass The containing light class
 * @param ownFunction The Kotlin function symbol to process
 * @param kotlinCollectionFunction The overridden symbol from Kotlin collections
 * @param allSupertypes All supertypes of the containing class
 * @param result Mutable list where generated PSI methods are added
 * @param originKind The origin kind of the original Kotlin function symbol
 * @return `true` if the caller should generate the original Kotlin method, `false` if it should be skipped
 *
 * @see tryToMapKotlinCollectionMethodToJavaMethod
 */
internal fun KaSession.processPossiblyMappedMethod(
    containingClass: SymbolLightClassForClassOrObject,
    ownFunction: KaNamedFunctionSymbol,
    kotlinCollectionFunction: KaNamedFunctionSymbol,
    allSupertypes: List<KaClassType>,
    result: MutableList<PsiMethod>,
    originKind: JvmDeclarationOriginKind,
): Boolean {
    val javaMethod = tryToMapKotlinCollectionMethodToJavaMethod(kotlinCollectionFunction, allSupertypes) ?: return true
    val collectionSupertype = allSupertypes.find { it.classId == kotlinCollectionFunction.callableId?.classId } ?: return true
    val javaCollection = javaMethod.containingClass ?: return true
    val substitutor = createPsiSubstitutor(javaCollection, collectionSupertype, containingClass)
    val isErasedSignature = javaMethod.name in ERASED_COLLECTION_METHOD_NAMES ||
            ownFunction.valueParameters.any { it.returnType is KaTypeParameterType }

    val lightMemberOrigin = (ownFunction.psi as? KtDeclaration)?.let { originalElement ->
        LightMemberOriginForDeclaration(originalElement, originKind)
    }
    val wrappedMethod = javaMethod.wrap(
        containingClass,
        substitutor,
        lightMemberOrigin,
        hasImplementation = true,
        makeFinal = !isErasedSignature
    )

    result.add(wrappedMethod)
    return !isErasedSignature
}

/**
 * Generates stub methods from Java collection interfaces for the first non-interface Kotlin class
 * that extends a mapped Kotlin collection type.
 *
 * This method is responsible for bridging the gap between Kotlin collection interfaces (like
 * `kotlin.collections.List`, `kotlin.collections.Map`) and their Java counterparts (like
 * `java.util.List`, `java.util.Map`) when a concrete class is defined.
 *
 * ## Why this is needed
 *
 * Kotlin collection interfaces are mapped to Java collection interfaces, but they don't declare
 * all the methods from the Java interfaces (for example, read-only Kotlin collections don't declare mutating methods).
 * When a Kotlin class directly implements a Kotlin collection interface (e.g., `class MyList : List<String>`), it becomes the first class
 * in the inheritance hierarchy. It must generate implementations for ALL methods from the corresponding Java interface
 * if the user cannot provide such implementations manually (for example, you can't override mutating Java methods
 * in a read-only Kotlin collection).
 *
 * ## Conditions for stub generation
 *
 * Stubs are only generated when ALL the following conditions are met:
 * 1. The class is a concrete/abstract class or object
 * 2. At least one supertype is a Kotlin collection that maps to a Java collection
 * 3. This class is the first non-interface subtype of Kotlin Collection in the inheritance chain (all other supertypes either
 * do not extend Collection or are interfaces)
 *
 * ## What gets generated
 *
 * For each method from the Java collection interface that isn't already present:
 * - Methods that map to Kotlin properties (e.g., `size()` → `size` property): generates both
 *   a final bridge method with the Java name and an abstract/concrete method with the Kotlin name (`getSize`)
 * - Methods not in the Kotlin interface: generates open bridge methods
 * - Methods in the Kotlin interface with special signatures: generates appropriate bridge methods with type substitution
 *
 * Generated stubs are added to the result list only if their signatures don't already exist.
 *
 * ## Example
 *
 * ```kotlin
 * class MyList : List<String> { ... }
 * ```
 *
 * This generates stubs for Java methods like `add(E)`, `remove(Object)`, `size()`, etc.,
 * with appropriate type substitution (`Object` → `String`).
 *
 * @param containingClass The containing light class
 * @param result Mutable list where generated stub methods are added
 * @param classSymbol The class symbol being processed
 * @param allSupertypes All supertypes of the class, used to find the closest mapped collection type
 *
 * @see generateJavaCollectionMethodStubs
 * @see mapKotlinClassToJava
 */
internal fun KaSession.generateJavaCollectionMethodStubsIfNeeded(
    containingClass: SymbolLightClassForClassOrObject,
    classSymbol: KaNamedClassSymbol,
    allSupertypes: List<KaClassType>,
    result: MutableList<PsiMethod>,
) {
    val closestMappedSupertype = allSupertypes.find { mapKotlinClassToJava(it.classId) != null } ?: return
    if (!isFirstNonInterfaceSubtypeOfCollection(classSymbol)) return

    val javaClassId = mapKotlinClassToJava(closestMappedSupertype.classId) ?: return
    val kotlinCollectionSymbol = closestMappedSupertype.symbol as? KaClassSymbol ?: return
    val javaCollectionSymbol = findClass(javaClassId) ?: return
    val javaCollectionPsiClass = javaCollectionSymbol.psi as? PsiClass ?: return
    val substitutor = createPsiSubstitutor(javaCollectionPsiClass, closestMappedSupertype, containingClass)

    generateJavaCollectionMethodStubs(containingClass, javaCollectionPsiClass, kotlinCollectionSymbol, substitutor, result)
}

private fun KaSession.isFirstNonInterfaceSubtypeOfCollection(classSymbol: KaClassSymbol): Boolean {
    if (classSymbol.classKind.let { it != KaClassKind.CLASS && it != KaClassKind.OBJECT }) {
        return false
    }

    return classSymbol.superTypes.none { directSupertype ->
        val supertypeSymbol = directSupertype.expandedSymbol ?: return@none false
        if (supertypeSymbol.classKind == KaClassKind.INTERFACE) return@none false
        val allSupertypes = supertypeSymbol.defaultType.allSupertypes.filterIsInstance<KaClassType>().toList()
        hasCollectionSupertype(allSupertypes)
    }
}

private fun mapKotlinClassToJava(classId: ClassId): ClassId? {
    return JavaToKotlinClassMap.mutabilityMappings.find {
        classId == it.kotlinReadOnly || classId == it.kotlinMutable
    }?.javaClass
}

private fun KaSession.createPsiSubstitutor(
    javaCollection: PsiClass,
    kotlinCollection: KaClassType,
    containingClass: SymbolLightClassForClassOrObject,
): PsiSubstitutor {
    val substitutionMap = buildMap<PsiTypeParameter, PsiType> {
        javaCollection.typeParameters.zip(kotlinCollection.typeArguments).forEach { (typeParameter, typeArgument) ->
            val psiType = typeArgument.type?.asPsiType(useSitePosition = containingClass, allowErrorTypes = true) ?: return@forEach
            put(typeParameter, psiType)
        }
    }
    return PsiSubstitutor.createSubstitutor(substitutionMap)
}

private fun KaSession.generateJavaCollectionMethodStubs(
    containingClass: SymbolLightClassForClassOrObject,
    javaCollectionPsiClass: PsiClass,
    kotlinCollectionSymbol: KaClassSymbol,
    substitutor: PsiSubstitutor,
    result: MutableList<PsiMethod>,
) {
    val kotlinNames = kotlinCollectionSymbol.memberScope.callables
        .filter { it is KaNamedFunctionSymbol }
        // skip default methods in Java collection
        .filter { it.origin != KaSymbolOrigin.JAVA_SOURCE && it.origin != KaSymbolOrigin.JAVA_LIBRARY }
        .mapNotNull { it.name?.asString() }
        .toSet()

    val javaMethods = javaCollectionPsiClass.methods
        .filterNot { it.hasModifierProperty(PsiModifier.DEFAULT) }

    val candidateMethods = javaMethods.flatMap { method ->
        createWrappersForJavaCollectionMethod(containingClass, method, javaCollectionPsiClass, kotlinNames, substitutor)
    }
    val existingSignatures = result.map { it.getSignature(substitutor) }.toSet()

    result += candidateMethods.filter { candidateMethod ->
        candidateMethod.getSignature(substitutor) !in existingSignatures
    }
}

private fun createWrappersForJavaCollectionMethod(
    containingClass: SymbolLightClassForClassOrObject,
    method: PsiMethod,
    javaCollectionPsiClass: PsiClass,
    kotlinNames: Set<String>,
    substitutor: PsiSubstitutor,
): List<PsiMethod> {
    val methodName = method.name

    val kotlinGetterNameWithDifferentAbi = JAVA_GETTER_NAME_TO_KOTLIN_GETTER_NAME_WITH_DIFFERENT_ABI[methodName]
    val hasCorrespondingKotlinDeclaration = methodName in kotlinNames || methodName in JAVA_MAP_ENTRY_METHODS
    val isSpecialNotErasedSignature = methodName in MEMBERS_WITH_SPECIAL_SIGNATURE && methodName !in ERASED_COLLECTION_METHOD_NAMES

    return when {
        kotlinGetterNameWithDifferentAbi != null -> {
            val hasImplementation = methodName == "size" && containingClass.withClassSymbol { classSymbol ->
                classSymbol.delegatedMemberScope.callables(Name.identifier("size")).toList().isNotEmpty()
            }
            val finalBridgeForJava = method.finalBridge(containingClass, substitutor)
            val abstractKotlinGetter = method.wrap(
                containingClass,
                substitutor,
                name = kotlinGetterNameWithDifferentAbi,
                hasImplementation = hasImplementation
            )

            listOf(finalBridgeForJava, abstractKotlinGetter)
        }

        hasCorrespondingKotlinDeclaration -> {
            if (isSpecialNotErasedSignature) {
                createMethodsWithSpecialSignature(containingClass, method, javaCollectionPsiClass, substitutor)
            } else {
                emptyList()
            }
        }

        else -> {
            val stubOverrideOfJavaOnlyMethod = method.openBridge(containingClass, substitutor)
            listOf(stubOverrideOfJavaOnlyMethod)
        }
    }
}

private fun createMethodsWithSpecialSignature(
    containingClass: SymbolLightClassForClassOrObject,
    method: PsiMethod,
    javaCollectionPsiClass: PsiClass,
    substitutor: PsiSubstitutor,
): List<PsiMethod> {
    // Case 1: two type parameters
    if (javaCollectionPsiClass.qualifiedName == CommonClassNames.JAVA_UTIL_MAP) {
        val abstractKotlinVariantWithGeneric = createJavaUtilMapMethodWithSpecialSignature(containingClass, method, substitutor)
            ?: return emptyList()
        val finalBridgeWithObject = method.finalBridge(containingClass, substitutor)
        return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
    }

    // Remaining cases: one type parameter
    if (method.name == "remove") {
        if (method.parameterList.parameters.singleOrNull()?.type == PsiTypes.intType()) {
            // remove(int) -> final bridge remove(int), abstract removeAt(int)
            return listOf(
                method.finalBridge(containingClass, substitutor),
                method.wrap(containingClass, substitutor, name = "removeAt")
            )
        } else if (javaCollectionPsiClass.qualifiedName == CommonClassNames.JAVA_UTIL_ITERATOR) {
            // skip default method java.util.Iterator#remove()
            return emptyList()
        }
    }

    val psiType = substitutor.substitutionMap.values.singleOrNull() ?: return emptyList()
    if (psiType.isTypeParameter()) return emptyList()

    val finalBridgeWithObject = method.finalBridge(containingClass, substitutor)
    val abstractKotlinVariantWithGeneric = method.wrap(containingClass, substitutor, substituteObjectWith = psiType)
    return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
}

private fun PsiType.isTypeParameter(): Boolean =
    this is PsiClassType && this.resolve() is PsiTypeParameter

private fun createJavaUtilMapMethodWithSpecialSignature(
    containingClass: SymbolLightClassForClassOrObject,
    method: PsiMethod,
    substitutor: PsiSubstitutor,
): SymbolLightMethodForMappedClass? {
    val typeParameters = substitutor.substitutionMap.keys
    val kOriginal = substitutor.substitutionMap[typeParameters.find { it.name == "K" }] ?: return null
    val vOriginal = substitutor.substitutionMap[typeParameters.find { it.name == "V" }] ?: return null
    val k = substitutor.substitute(kOriginal) ?: kOriginal
    val v = substitutor.substitute(vOriginal) ?: vOriginal

    val signature = when (method.name) {
        "get" -> {
            if (k.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(k), returnType = v)
        }

        "containsKey" -> {
            if (k.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(k), returnType = PsiTypes.booleanType())
        }

        "containsValue" -> {
            if (v.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(v), returnType = PsiTypes.booleanType())
        }

        "remove" -> {
            // only `remove(Object)` pair (i.e. `remove(K)`) is needed
            if (method.parameterList.parametersCount != 1) return null
            if (k.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(k), returnType = v)
        }
        else -> null
    } ?: return null

    return method.wrap(containingClass, substitutor, signature = signature)
}

private fun PsiMethod.finalBridge(
    containingClass: SymbolLightClassForClassOrObject,
    substitutor: PsiSubstitutor,
): SymbolLightMethodForMappedClass = wrap(containingClass, substitutor, makeFinal = true, hasImplementation = true)

private fun PsiMethod.openBridge(
    containingClass: SymbolLightClassForClassOrObject,
    substitutor: PsiSubstitutor,
): SymbolLightMethodForMappedClass = wrap(containingClass, substitutor, makeFinal = false, hasImplementation = true)

private fun PsiMethod.wrap(
    containingClass: SymbolLightClassForClassOrObject,
    substitutor: PsiSubstitutor,
    lightMemberOrigin: LightMemberOrigin? = null,
    makeFinal: Boolean = false,
    hasImplementation: Boolean = false,
    name: String = this.name,
    substituteObjectWith: PsiType? = null,
    signature: MethodSignature? = null,
) = SymbolLightMethodForMappedClass(
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    javaMethod = this,
    substitutor = substitutor,
    name = name,
    isFinal = makeFinal,
    hasImplementation = hasImplementation,
    substituteObjectWith = substituteObjectWith,
    providedSignature = signature
)

@Suppress("UnstableApiUsage")
private fun KaSession.tryToMapKotlinCollectionMethodToJavaMethod(
    kotlinCollectionFunction: KaNamedFunctionSymbol,
    allSupertypes: List<KaClassType>,
): PsiMethod? {
    val name = kotlinCollectionFunction.name.asString()
    if (name == "addAll") {
        // "addAll" has two overloads from different types with different number of parameters
        return if (kotlinCollectionFunction.valueParameters.size == 1) {
            getJavaCollectionClass(allSupertypes)?.methods?.find { it.name == name }
        } else {
            getJavaListClass(allSupertypes)?.methods?.find { it.name == name && it.parameters.size == 2 }
        }
    }

    val javaClass = when (name) {
        "contains", "containsAll", "removeAll", "retainAll" -> getJavaCollectionClass(allSupertypes)
        "indexOf", "lastIndexOf" -> getJavaListClass(allSupertypes)
        "get", "containsKey", "containsValue", "putAll" -> getJavaMapClass(allSupertypes)
        "remove" -> {
            if (kotlinCollectionFunction.callableId?.classId == StandardClassIds.MutableMap) {
                getJavaMapClass(allSupertypes)
            } else {
                getJavaCollectionClass(allSupertypes)
            }
        }
        else -> null
    }

    return javaClass?.methods?.find { it.name == name }
}

private fun KaSession.getJavaCollectionClass(allSupertypes: List<KaClassType>): PsiClass? =
    getJavaPsiClass(allSupertypes, kotlinClassId = StandardClassIds.Collection)

private fun KaSession.getJavaListClass(allSupertypes: List<KaClassType>): PsiClass? =
    getJavaPsiClass(allSupertypes, kotlinClassId = StandardClassIds.List)

private fun KaSession.getJavaMapClass(allSupertypes: List<KaClassType>): PsiClass? =
    getJavaPsiClass(allSupertypes, kotlinClassId = StandardClassIds.Map)

private fun KaSession.getJavaPsiClass(allSupertypes: List<KaClassType>, kotlinClassId: ClassId): PsiClass? {
    val kotlinCollection = allSupertypes.find { it.classId == kotlinClassId } ?: return null
    val javaClassId = mapKotlinClassToJava(kotlinCollection.classId) ?: return null
    val javaCollectionSymbol = findClass(javaClassId) ?: return null
    return javaCollectionSymbol.psi as? PsiClass
}

internal fun KaCallableSymbol.isFromKotlinCollectionsPackage(): Boolean =
    callableId?.packageName?.startsWith(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME) == true

private fun KaSession.findOverriddenCollectionSymbol(symbol: KaNamedFunctionSymbol): KaNamedFunctionSymbol? =
    symbol.allOverriddenSymbols.find { it.isFromKotlinCollectionsPackage() } as? KaNamedFunctionSymbol
