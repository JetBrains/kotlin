/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.asPsiType
import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.light.classes.symbol.methods.MethodSignature
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedJavaCollectionStubMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedKotlinCollectionMethod
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
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
    BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.forEach { [propertyFqName, javaGetterShortName] ->
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
 * @see processPossiblyMappedCollectionMethod
 */
context(_: KaSession)
internal fun processOwnDeclarationsMappedCollectionMethodsAware(
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

                val shouldCreateRegularDeclaration = processPossiblyMappedCollectionMethod(
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

internal fun hasCollectionSupertype(allSupertypes: Iterable<KaClassType>): Boolean =
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
 *    - Each parameter erased to `Object` in Java has a type parameter type, which gets erased as well
 *      (e.g., `contains(element: E)` for `contains(Object)`, or `getOrDefault(key: K, defaultValue: String)` for `getOrDefault(Object, V)`)
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
context(_: KaSession)
internal fun processPossiblyMappedCollectionMethod(
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
            javaMethod.parameterList.parameters.zip(ownFunction.valueParameters).all { [javaParameter, parameter] ->
                !javaParameter.type.isJavaLangObject() || parameter.returnType is KaTypeParameterType
            }

    val lightMemberOrigin = (ownFunction.psi as? KtDeclaration)?.let { originalElement ->
        LightMemberOriginForDeclaration(originalElement, originKind)
    }

    val mappedMethod = SymbolLightMethodForMappedKotlinCollectionMethod(
        functionSymbol = ownFunction,
        lightMemberOrigin = lightMemberOrigin,
        containingClass = containingClass,
        javaMethod = javaMethod,
        substitutor = substitutor,
        isFinal = !isErasedSignature,
    )

    result.add(mappedMethod)
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
 * - Methods not in the Kotlin interface: generates open bridge methods.
 *   Java default methods get them only if the Kotlin interface is read-only and only its mutable counterpart has them
 *   (e.g., `removeIf`)
 * - Methods in the Kotlin interface with special signatures: generates appropriate bridge methods with type substitution
 *
 * Generated stubs are added to the result list only if their signatures don't already exist.
 *
 * A subclass of such a class only gets special bridges for the members it inherits with substituted types, see
 * [generateSpecialBridgesForInheritedMethods].
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
 * @see mapKotlinCollectionClassToJava
 */
context(session: KaSession)
internal fun generateJavaCollectionMethodStubsIfNeeded(
    containingClass: SymbolLightClassForClassOrObject,
    classSymbol: KaNamedClassSymbol,
    allSupertypes: List<KaClassType>,
    result: MutableList<PsiMethod>,
) {
    val closestMappedSupertype = findClosestMappedCollectionSupertype(allSupertypes) ?: return
    val javaClassId = mapKotlinCollectionClassToJava(closestMappedSupertype.classId) ?: return
    val kotlinCollectionSymbol = closestMappedSupertype.symbol as? KaClassSymbol ?: return
    val javaCollectionSymbol = findClass(javaClassId) ?: return
    val javaCollectionPsiClass = javaCollectionSymbol.psi as? PsiClass ?: return
    val substitutor = createPsiSubstitutor(javaCollectionPsiClass, closestMappedSupertype, containingClass)

    if (isFirstNonInterfaceSubtypeOfCollection(classSymbol)) {
        generateJavaCollectionMethodStubs(containingClass, javaCollectionPsiClass, javaClassId, kotlinCollectionSymbol, substitutor, result)
    } else {
        generateSpecialBridgesForInheritedMethods(containingClass, classSymbol, javaCollectionPsiClass, substitutor, result)
    }
}

/**
 * Chooses the first mapped collection supertype that is not shadowed by a more specific mapped one.
 *
 * This avoids stopping at `Collection` when the same class also inherits from ex. `List`, so light classes
 * can generate the richer `List` stub set.
 */
context(session: KaSession)
private fun findClosestMappedCollectionSupertype(allSupertypes: List<KaClassType>): KaClassType? {
    val mappedSupertypes = allSupertypes.filter { mapKotlinCollectionClassToJava(it.classId) != null }

    return mappedSupertypes.firstOrNull { candidate ->
        mappedSupertypes.none { other ->
            other !== candidate && other.isSubtypeOf(candidate) && !candidate.isSubtypeOf(other)
        }
    }
}

context(session: KaSession)
private fun isFirstNonInterfaceSubtypeOfCollection(classSymbol: KaClassSymbol): Boolean {
    return classSymbol.superTypes.none { directSupertype ->
        val supertypeSymbol = directSupertype.expandedSymbol ?: return@none false
        if (supertypeSymbol.classKind == KaClassKind.INTERFACE) return@none false
        val allSupertypes = supertypeSymbol.defaultType.allSupertypes.filterIsInstance<KaClassType>().asIterable()
        hasCollectionSupertype(allSupertypes)
    }
}

private fun mapKotlinCollectionClassToJava(classId: ClassId): ClassId? {
    return JavaToKotlinClassMap.mutabilityMappings.find {
        classId == it.kotlinReadOnly || classId == it.kotlinMutable
    }?.javaClass
}

context(session: KaSession)
private fun createPsiSubstitutor(
    javaCollection: PsiClass,
    kotlinCollection: KaClassType,
    containingClass: SymbolLightClassForClassOrObject,
): PsiSubstitutor = with(session) {
    val substitutionMap = buildMap<PsiTypeParameter, PsiType> {
        javaCollection.typeParameters.zip(kotlinCollection.typeArguments).forEach { [typeParameter, typeArgument] ->
            val psiType = typeArgument.type?.asPsiType(
                useSitePosition = containingClass,
                allowErrorTypes = true,
                mode = KaTypeMappingMode.GENERIC_ARGUMENT
            ) ?: return@forEach
            put(typeParameter, psiType)
        }
    }
    return PsiSubstitutor.createSubstitutor(substitutionMap)
}

context(session: KaSession)
private fun generateJavaCollectionMethodStubs(
    containingClass: SymbolLightClassForClassOrObject,
    javaCollectionPsiClass: PsiClass,
    javaClassId: ClassId,
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

    val isReadOnlyCollection = JavaToKotlinClassMap.isReadOnly(kotlinCollectionSymbol.classId)
    val javaMethods = javaCollectionPsiClass.methods.filter { method ->
        when {
            method.hasModifierProperty(PsiModifier.STATIC) -> false
            // The JVM backend generates stubs only for the default methods that read-only Kotlin collections don't have
            method.hasModifierProperty(PsiModifier.DEFAULT) -> isReadOnlyCollection && method.isMutableOnlyJdkMethod(javaClassId)
            else -> true
        }
    }

    val candidateMethods = javaMethods.flatMap { method ->
        createWrappersForJavaCollectionMethod(containingClass, method, javaCollectionPsiClass, kotlinNames, substitutor)
    }
    val existingSignatures = result.mapTo(HashSet()) { it.getSignature(substitutor) }
    result += candidateMethods.filter { candidateMethod ->
        candidateMethod.getSignature(substitutor) !in existingSignatures
    }
}

/**
 * Whether only the mutable Kotlin counterpart of the JDK collection [javaClassId] has this method,
 * like `removeIf` of `java.util.Collection`, which `kotlin.collections.MutableCollection` has, but `kotlin.collections.Collection` doesn't.
 *
 * @see JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES
 */
private fun PsiMethod.isMutableOnlyJdkMethod(javaClassId: ClassId): Boolean {
    val jvmDescriptor = name + ClassUtil.getAsmMethodSignature(this)
    return SignatureBuildingComponents.signature(javaClassId, jvmDescriptor) in JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES
}

/**
 * Generates special bridges for the collection members that [classSymbol] inherits from its superclass.
 *
 * The JVM backend generates special bridges for fake overrides as it does for explicit overrides, see `BridgeLowering`. For example:
 *
 * ```kotlin
 * abstract class StringList : AbstractList<String>()
 * ```
 *
 * `AbstractList<E>` has no special bridge for `contains(element: E)`, as its JVM signature is the Java one, `contains(Object)`.
 * `StringList` inherits `contains(element: String)`, so it gets an open `contains(String)`, which calls the inherited implementation,
 * and the final bridge `contains(Object)`. If the inherited member is abstract, its Kotlin variant is abstract too, like
 * `removeAt(int)` in a subclass of `AbstractMutableList<String>`.
 *
 * A member gets no bridges if a Kotlin superclass already has a special bridge with the same JVM signature, even if the backend
 * didn't generate it there because the member of that superclass comes from an interface. For example, a subclass of
 * `abstract class StringMap : Map<String, String>` gets no `getOrDefault` bridges, while `StringMap` doesn't have them either.
 */
context(session: KaSession)
private fun generateSpecialBridgesForInheritedMethods(
    containingClass: SymbolLightClassForClassOrObject,
    classSymbol: KaNamedClassSymbol,
    javaCollectionPsiClass: PsiClass,
    substitutor: PsiSubstitutor,
    result: MutableList<PsiMethod>,
) {
    for (javaMethod in javaCollectionPsiClass.methods) {
        val kotlinName = javaMethod.kotlinNameIfHasSpecialBridge() ?: continue
        val function = classSymbol.findCollectionFunction(kotlinName, javaMethod) ?: continue

        val isOwnDeclaration = function.containingSymbol == classSymbol &&
                function.origin != KaSymbolOrigin.SUBSTITUTION_OVERRIDE &&
                function.origin != KaSymbolOrigin.INTERSECTION_OVERRIDE

        // Own overrides and delegates get their bridges along with the other declarations, see `processPossiblyMappedCollectionMethod`,
        // except for the bridge `remove(int)` of `removeAt`, which has a different name
        if (isOwnDeclaration && kotlinName != "removeAt") continue

        val signatures = computeSpecialBridgeSignatures(function, javaMethod, containingClass)
        if (signatures.bridge == signatures.member) continue
        if (signatures.bridge in collectSpecialBridgeSignaturesOfSuperclasses(classSymbol, kotlinName, javaMethod, containingClass)) {
            continue
        }

        val bridgeSignature = computeFinalBridgeSignature(function, javaMethod, containingClass)
        result += javaMethod.wrap(containingClass, substitutor, makeFinal = true, hasImplementation = true, signature = bridgeSignature)
        if (isOwnDeclaration) continue

        // An open bridge calling the inherited implementation has no generic signature, unlike an abstract stub
        val hasImplementation = function.modality != KaSymbolModality.ABSTRACT
        val kotlinSignature = computeKotlinVariantSignature(function, javaMethod, containingClass, erase = hasImplementation)
        result += javaMethod.wrap(
            containingClass,
            substitutor,
            hasImplementation = hasImplementation,
            name = kotlinName,
            signature = kotlinSignature,
        )
    }
}

/**
 * The name of the Kotlin collection function corresponding to this Java collection method if the JVM backend generates
 * special bridges for it.
 *
 * These are the methods with an erased `Object` parameter, like `contains(Object)` and `getOrDefault(Object, V)`, and `remove(int)`,
 * which is `removeAt` in Kotlin. Methods with an erased collection parameter, like `containsAll(Collection<?>)`, are not special,
 * as their Kotlin variants have the same JVM signature.
 */
private fun PsiMethod.kotlinNameIfHasSpecialBridge(): String? {
    val parameterTypes = parameterList.parameters.map { it.type }
    return when {
        name == "remove" && parameterTypes.singleOrNull() == PsiTypes.intType() -> "removeAt"
        name in MEMBERS_WITH_SPECIAL_SIGNATURE && parameterTypes.any { it.isJavaLangObject() } -> name
        else -> null
    }
}

/**
 * The member function of this class named [kotlinName] that overrides a Kotlin collection function mapped to [javaMethod].
 */
context(session: KaSession)
private fun KaClassSymbol.findCollectionFunction(kotlinName: String, javaMethod: PsiMethod): KaNamedFunctionSymbol? =
    memberScope.callables(Name.identifier(kotlinName))
        .filterIsInstance<KaNamedFunctionSymbol>()
        .firstOrNull { function ->
            function.valueParameters.size == javaMethod.parameterList.parametersCount &&
                    (function.isFromKotlinCollectionsPackage() || function.allOverriddenSymbols.any { it.isFromKotlinCollectionsPackage() })
        }

/**
 * The erased JVM signatures that the JVM backend compares to decide whether a class needs special bridges for a collection function.
 *
 * @property member the signature of the Kotlin function itself, like `contains(String)` for `contains(element: String)`
 * @property bridge the signature of its special bridge, like `contains(Object)`
 * @property unsubstitutedBridge the signature of its special bridge with an erased return type, like `get(Object): Object` for
 * `get(Object): String`, if the return type of the bridge is substituted
 */
private class SpecialBridgeSignatures(
    val member: ErasedSignature,
    val bridge: ErasedSignature,
    val unsubstitutedBridge: ErasedSignature?,
)

private data class ErasedSignature(val name: String, val parameterTypes: List<String>, val returnType: String)

/**
 * Computes [SpecialBridgeSignatures] of [function], a Kotlin variant of [javaMethod] as seen in some class.
 *
 * The types of [function] may refer to the type parameters of a superclass, so they are erased before they are mapped to Java.
 */
context(session: KaSession)
private fun computeSpecialBridgeSignatures(
    function: KaNamedFunctionSymbol,
    javaMethod: PsiMethod,
    useSitePosition: PsiElement,
): SpecialBridgeSignatures {
    fun KaType.erasedText(mode: KaTypeMappingMode): String =
        erasedPsiType(useSitePosition, mode)?.canonicalText ?: CommonClassNames.JAVA_LANG_OBJECT

    val javaParameters = javaMethod.parameterList.parameters
    val memberParameterTypes = function.valueParameters.map { it.returnType.erasedText(KaTypeMappingMode.VALUE_PARAMETER) }
    val bridgeParameterTypes = javaParameters.zip(memberParameterTypes) { javaParameter, memberParameterType ->
        if (javaParameter.type.isJavaLangObject()) CommonClassNames.JAVA_LANG_OBJECT else memberParameterType
    }

    val javaReturnType = javaMethod.returnType ?: PsiTypes.voidType()
    val hasSubstitutedReturnType = javaReturnType.isTypeParameter()
    val memberReturnType: String
    val bridgeReturnType: String
    if (hasSubstitutedReturnType) {
        memberReturnType = function.returnType.erasedText(KaTypeMappingMode.RETURN_TYPE_BOXED)
        bridgeReturnType = function.returnType.erasedText(javaMethod.bridgeReturnTypeMappingMode)
    } else {
        memberReturnType = javaReturnType.canonicalText
        bridgeReturnType = javaReturnType.canonicalText
    }

    return SpecialBridgeSignatures(
        member = ErasedSignature(function.name.asString(), memberParameterTypes, memberReturnType),
        bridge = ErasedSignature(javaMethod.name, bridgeParameterTypes, bridgeReturnType),
        unsubstitutedBridge = if (hasSubstitutedReturnType) {
            val erasedJavaParameterTypes = javaParameters.map { TypeConversionUtil.erasure(it.type).canonicalText }
            ErasedSignature(javaMethod.name, erasedJavaParameterTypes, CommonClassNames.JAVA_LANG_OBJECT)
        } else {
            null
        },
    )
}

/**
 * The signatures of the special bridges for [javaMethod] that the Kotlin superclasses of [classSymbol] have, or would have if their
 * members didn't come from an interface.
 *
 * Java classes have no special bridges, but a Kotlin class above them in the hierarchy may have.
 */
context(session: KaSession)
private fun collectSpecialBridgeSignaturesOfSuperclasses(
    classSymbol: KaClassSymbol,
    kotlinName: String,
    javaMethod: PsiMethod,
    useSitePosition: PsiElement,
): Set<ErasedSignature> = buildSet {
    for (superclass in generateSequence(classSymbol.superclass) { it.superclass }) {
        if (superclass.origin == KaSymbolOrigin.JAVA_SOURCE || superclass.origin == KaSymbolOrigin.JAVA_LIBRARY) continue
        val function = superclass.findCollectionFunction(kotlinName, javaMethod) ?: continue
        val signatures = computeSpecialBridgeSignatures(function, javaMethod, useSitePosition)
        if (signatures.bridge != signatures.member) add(signatures.bridge)
        if (signatures.unsubstitutedBridge != null && signatures.unsubstitutedBridge != signatures.member) {
            add(signatures.unsubstitutedBridge)
        }
    }
}

context(session: KaSession)
private val KaClassSymbol.superclass: KaClassSymbol?
    get() = superTypes.firstNotNullOfOrNull { supertype ->
        supertype.expandedSymbol?.takeIf { it.classKind != KaClassKind.INTERFACE }
    }

/**
 * The signature of the final special bridge for [function], an inherited Kotlin variant of [javaMethod], like
 * `getOrDefault(Object, String)` for `getOrDefault(key: String, defaultValue: String)`.
 *
 * Only the erased `Object` parameters of [javaMethod] stay as they are.
 */
context(session: KaSession)
private fun computeFinalBridgeSignature(
    function: KaNamedFunctionSymbol,
    javaMethod: PsiMethod,
    containingClass: SymbolLightClassForClassOrObject,
): MethodSignature {
    val parameterTypes = javaMethod.parameterList.parameters.zip(function.valueParameters) { javaParameter, parameter ->
        if (javaParameter.type.isJavaLangObject()) {
            javaParameter.type
        } else {
            parameter.returnType.mapToPsiType(containingClass, KaTypeMappingMode.VALUE_PARAMETER) ?: javaParameter.type
        }
    }

    val javaReturnType = javaMethod.returnType ?: PsiTypes.voidType()
    val returnType = if (javaReturnType.isTypeParameter()) {
        function.returnType.mapToPsiType(containingClass, javaMethod.bridgeReturnTypeMappingMode) ?: javaReturnType
    } else {
        javaReturnType
    }

    return MethodSignature(parameterTypes, returnType)
}

/**
 * The signature of the Kotlin variant of [javaMethod] for [function], an inherited Kotlin collection function,
 * like `getOrDefault(String, String)` for `getOrDefault(key: String, defaultValue: String)`.
 *
 * If [erase] is `true`, the type parameters in the signature are replaced with their erasure.
 */
context(session: KaSession)
private fun computeKotlinVariantSignature(
    function: KaNamedFunctionSymbol,
    javaMethod: PsiMethod,
    containingClass: SymbolLightClassForClassOrObject,
    erase: Boolean,
): MethodSignature {
    fun KaType.toPsiType(mode: KaTypeMappingMode): PsiType? =
        if (erase) erasedPsiType(containingClass, mode) else mapToPsiType(containingClass, mode)

    val parameterTypes = javaMethod.parameterList.parameters.zip(function.valueParameters) { javaParameter, parameter ->
        parameter.returnType.toPsiType(KaTypeMappingMode.VALUE_PARAMETER) ?: javaParameter.type
    }

    val javaReturnType = javaMethod.returnType ?: PsiTypes.voidType()
    val returnType = if (javaReturnType.isTypeParameter()) {
        function.returnType.toPsiType(KaTypeMappingMode.RETURN_TYPE_BOXED) ?: javaReturnType
    } else {
        javaReturnType
    }

    return MethodSignature(parameterTypes, returnType)
}

/**
 * The JVM backend keeps a primitive element type in the return type of the special bridge `remove(int)`, like in `int remove(int)`
 * for `removeAt(index: Int): Int`, and boxes the substituted return types of the other special bridges.
 */
private val PsiMethod.bridgeReturnTypeMappingMode: KaTypeMappingMode
    get() = if (name == "remove" && parameterList.parameters.singleOrNull()?.type == PsiTypes.intType()) {
        KaTypeMappingMode.RETURN_TYPE
    } else {
        KaTypeMappingMode.RETURN_TYPE_BOXED
    }

context(session: KaSession)
private fun KaType.mapToPsiType(useSitePosition: PsiElement, mode: KaTypeMappingMode): PsiType? =
    asPsiType(useSitePosition, allowErrorTypes = true, mode = mode)

/**
 * The erasure of this type mapped to Java, like `Object` for an unbounded type parameter and `CharSequence` for `E : CharSequence`.
 *
 * A flexible type parameter type, like `K!` in a member inherited from a Java class, is erased like the type parameter itself.
 */
context(session: KaSession)
private fun KaType.erasedPsiType(useSitePosition: PsiElement, mode: KaTypeMappingMode): PsiType? {
    var type = (this as? KaFlexibleType)?.lowerBound?.takeIf { it is KaTypeParameterType } ?: this
    while (type is KaTypeParameterType) {
        type = type.symbol.upperBounds.firstOrNull() ?: builtinTypes.nullableAny
    }
    return type.mapToPsiType(useSitePosition, mode)?.let(TypeConversionUtil::erasure)
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
            val finalBridgeForJava = method.finalBridge(containingClass, substitutor)
            val abstractKotlinGetter = method.wrap(
                containingClass,
                substitutor,
                name = kotlinGetterNameWithDifferentAbi,
                hasImplementation = false
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
    val javaParameterType = method.parameterList.parameters.singleOrNull()?.type
    if (method.name == "remove" && javaParameterType == PsiTypes.intType()) {
        // remove(int) -> final bridge remove(int), abstract removeAt(int)
        return listOf(
            method.finalBridge(containingClass, substitutor),
            method.wrap(containingClass, substitutor, name = "removeAt")
        )
    }

    // Only a method with the erased element parameter, like `contains(Object)`, has a Kotlin variant with a different signature.
    // For example, `get(int)` of `java.util.List` and `remove()` of `java.util.ListIterator` are the same in Kotlin,
    // unlike `get(Object)` of `java.util.Map` handled above.
    if (javaParameterType == null || !javaParameterType.isJavaLangObject()) return emptyList()

    val psiType = substitutor.substitutionMap.values.singleOrNull() ?: return emptyList()
    if (psiType.isTypeParameter()) return emptyList()

    val finalBridgeWithObject = method.finalBridge(containingClass, substitutor)
    val abstractKotlinVariantWithGeneric = method.wrap(containingClass, substitutor, substituteObjectWith = psiType)
    return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
}

internal fun PsiType.isTypeParameter(): Boolean =
    this is PsiClassType && this.resolve() is PsiTypeParameter

/**
 * Returns the primitive type for this boxed primitive type, e.g., `int` for `Integer`, or this type if it isn't one.
 *
 * A type annotated with `@Nullable` stays boxed: it comes from a nullable Kotlin type, like `Int?` in `List<Int?>`,
 * which can't be primitive.
 */
internal fun PsiType.unboxedOrSelf(): PsiType {
    if (hasAnnotation(JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION.asString())) return this
    return PsiPrimitiveType.getUnboxedType(this)?.annotate(TypeAnnotationProvider.EMPTY) ?: this
}

internal fun PsiType.isJavaLangObject(): Boolean =
    this is PsiClassType && this.canonicalText == CommonClassNames.JAVA_LANG_OBJECT

private fun createJavaUtilMapMethodWithSpecialSignature(
    containingClass: SymbolLightClassForClassOrObject,
    method: PsiMethod,
    substitutor: PsiSubstitutor,
): SymbolLightMethodForMappedJavaCollectionStubMethod? {
    val typeParameters = substitutor.substitutionMap.keys
    val kOriginal = substitutor.substitutionMap[typeParameters.find { it.name == "K" }] ?: return null
    val vOriginal = substitutor.substitutionMap[typeParameters.find { it.name == "V" }] ?: return null
    val k = substitutor.substitute(kOriginal) ?: kOriginal
    val v = substitutor.substitute(vOriginal) ?: vOriginal

    // The Kotlin variant takes the key and the value types unboxed, like `get(int)` in `Map<Int, V>`
    val signature = when (method.name) {
        "get" -> {
            if (k.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(k.unboxedOrSelf()), returnType = v)
        }

        "containsKey" -> {
            if (k.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(k.unboxedOrSelf()), returnType = PsiTypes.booleanType())
        }

        "containsValue" -> {
            if (v.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(v.unboxedOrSelf()), returnType = PsiTypes.booleanType())
        }

        "remove" -> {
            // only `remove(Object)` pair (i.e. `remove(K)`) is needed
            if (method.parameterList.parametersCount != 1) return null
            if (k.isTypeParameter()) return null
            MethodSignature(parameterTypes = listOf(k.unboxedOrSelf()), returnType = v)
        }
        else -> null
    } ?: return null

    return method.wrap(containingClass, substitutor, signature = signature)
}

private fun PsiMethod.finalBridge(
    containingClass: SymbolLightClassForClassOrObject,
    substitutor: PsiSubstitutor,
): SymbolLightMethodForMappedJavaCollectionStubMethod = wrap(containingClass, substitutor, makeFinal = true, hasImplementation = true)

private fun PsiMethod.openBridge(
    containingClass: SymbolLightClassForClassOrObject,
    substitutor: PsiSubstitutor,
): SymbolLightMethodForMappedJavaCollectionStubMethod = wrap(containingClass, substitutor, makeFinal = false, hasImplementation = true)

private fun PsiMethod.wrap(
    containingClass: SymbolLightClassForClassOrObject,
    substitutor: PsiSubstitutor,
    makeFinal: Boolean = false,
    hasImplementation: Boolean = false,
    name: String = this.name,
    substituteObjectWith: PsiType? = null,
    signature: MethodSignature? = null,
) = SymbolLightMethodForMappedJavaCollectionStubMethod(
    containingClass = containingClass,
    javaMethod = this,
    classSubstitutor = substitutor,
    name = name,
    isFinal = makeFinal,
    hasImplementation = hasImplementation,
    substituteObjectWith = substituteObjectWith,
    providedSignature = signature
)

context(_: KaSession)
private fun tryToMapKotlinCollectionMethodToJavaMethod(
    kotlinCollectionFunction: KaNamedFunctionSymbol,
    allSupertypes: List<KaClassType>,
): PsiMethod? {
    val name = kotlinCollectionFunction.name.asString()
    val parameterCount = kotlinCollectionFunction.valueParameters.size

    fun PsiClass.findMatchingMethod(): PsiMethod? =
        methods.find { it.name == name && it.parameterList.parametersCount == parameterCount }

    val javaClass = when (name) {
        "addAll" -> {
            // "addAll" has two overloads from different types with different number of parameters
            if (parameterCount == 1) getJavaCollectionClass(allSupertypes) else getJavaListClass(allSupertypes)
        }
        "contains", "containsAll", "removeAll", "retainAll" -> getJavaCollectionClass(allSupertypes)
        "indexOf", "lastIndexOf" -> getJavaListClass(allSupertypes)
        "get", "containsKey", "containsValue", "getOrDefault", "putAll" -> getJavaMapClass(allSupertypes)
        "remove" -> {
            if (kotlinCollectionFunction.callableId?.classId == StandardClassIds.MutableMap) {
                getJavaMapClass(allSupertypes)
            } else {
                getJavaCollectionClass(allSupertypes)
            }
        }
        else -> null
    }

    return javaClass?.findMatchingMethod()
}

context(_: KaSession)
private fun getJavaCollectionClass(allSupertypes: List<KaClassType>): PsiClass? =
    getJavaPsiClass(allSupertypes, kotlinClassId = StandardClassIds.Collection)

context(_: KaSession)
private fun getJavaListClass(allSupertypes: List<KaClassType>): PsiClass? =
    getJavaPsiClass(allSupertypes, kotlinClassId = StandardClassIds.List)

context(_: KaSession)
private fun getJavaMapClass(allSupertypes: List<KaClassType>): PsiClass? =
    getJavaPsiClass(allSupertypes, kotlinClassId = StandardClassIds.Map)

context(session: KaSession)
private fun getJavaPsiClass(allSupertypes: List<KaClassType>, kotlinClassId: ClassId): PsiClass? {
    val kotlinCollection = allSupertypes.find { it.classId == kotlinClassId } ?: return null
    val javaClassId = mapKotlinCollectionClassToJava(kotlinCollection.classId) ?: return null
    val javaCollectionSymbol = findClass(javaClassId) ?: return null
    return javaCollectionSymbol.psi as? PsiClass
}

internal fun KaCallableSymbol.isFromKotlinCollectionsPackage(): Boolean =
    callableId?.packageName?.startsWith(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME) == true

context(session: KaSession)
private fun findOverriddenCollectionSymbol(symbol: KaNamedFunctionSymbol): KaNamedFunctionSymbol? =
    symbol.allOverriddenSymbols.find { it.isFromKotlinCollectionsPackage() } as? KaNamedFunctionSymbol
