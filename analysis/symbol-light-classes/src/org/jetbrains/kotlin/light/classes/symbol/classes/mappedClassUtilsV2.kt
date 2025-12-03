/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.light.classes.symbol.methods.KaMethodSignature
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedClassV2
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

/**
 * Alternative implementation of mappedClassUtils that uses KaSymbol abstractions instead of
 * PsiMethod and PsiSubstitutor.
 *
 * This version:
 * - Uses KaType for type information instead of PsiType
 * - Uses KaNamedFunctionSymbol instead of PsiMethod for representing Java collection methods
 * - Performs type substitution using KaType APIs
 * - Delays PsiType conversion until SymbolLightMethodForMappedClassV2 construction
 *
 * The goal is to align with the Symbol light classes architecture where:
 * 1. Symbol pointers are stored for all symbols
 * 2. Type information is kept as KaType
 * 3. PsiType conversion happens lazily via asPsiType()
 * 4. No direct dependencies on Psi* classes for core logic
 */

/**
 * Java getters with non-standard names that have corresponding Kotlin properties with different JVM ABI names.
 */
private val JAVA_GETTER_NAME_TO_KOTLIN_GETTER_NAME_WITH_DIFFERENT_ABI: Map<String, String> = buildMap {
    BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.forEach { (propertyFqName, javaGetterShortName) ->
        put(javaGetterShortName.asString(), JvmAbi.getterName(propertyFqName.shortName().asString()))
    }
}

/**
 * Java map entry methods that have corresponding Kotlin properties with the same JVM ABI name.
 */
private val JAVA_MAP_ENTRY_METHODS: Set<String> = setOf("getKey", "getValue")

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
 * Alternative version of processOwnDeclarationsMappedSpecialSignaturesAware using KaSymbol abstractions.
 *
 * @see processOwnDeclarationsMappedSpecialSignaturesAware
 */
internal fun KaSession.processOwnDeclarationsMappedSpecialSignaturesAwareV2(
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

                val shouldCreateRegularDeclaration = processPossiblyMappedMethodV2(
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

/**
 * Alternative version of processPossiblyMappedMethod using KaSymbol abstractions.
 *
 * Instead of looking up Java PsiMethod and creating PsiSubstitutor, this version:
 * 1. Looks up the Java collection function as KaNamedFunctionSymbol
 * 2. Uses KaType for type substitution
 * 3. Creates SymbolLightMethodForMappedClassV2 which internally handles PsiType conversion
 *
 * @see processPossiblyMappedMethod
 */
@OptIn(KaExperimentalApi::class)
internal fun KaSession.processPossiblyMappedMethodV2(
    containingClass: SymbolLightClassForClassOrObject,
    ownFunction: KaNamedFunctionSymbol,
    kotlinCollectionFunction: KaNamedFunctionSymbol,
    allSupertypes: List<KaClassType>,
    result: MutableList<PsiMethod>,
    originKind: JvmDeclarationOriginKind,
): Boolean {
    val javaFunctionSymbol = tryToMapKotlinCollectionMethodToJavaMethodSymbol(kotlinCollectionFunction, allSupertypes) ?: return true
    val javaClassSymbol = javaFunctionSymbol.containingSymbol as? KaClassSymbol ?: return true
    val kotlinCollectionType = allSupertypes.find { it.classId == kotlinCollectionFunction.callableId?.classId } ?: return true

    val isErasedSignature = javaFunctionSymbol.name.asString() in ERASED_COLLECTION_METHOD_NAMES ||
            ownFunction.valueParameters.any { it.returnType is KaTypeParameterType }

    val lightMemberOrigin = (ownFunction.psi as? KtDeclaration)?.let { originalElement ->
        LightMemberOriginForDeclaration(originalElement, originKind)
    }

    val substitutionMap = buildSubstitutionMap(javaClassSymbol, kotlinCollectionType)

    val wrappedMethod = javaFunctionSymbol.wrapAsSymbolMethod(
        containingClass = containingClass,
        lightMemberOrigin = lightMemberOrigin,
        substitutionMap = substitutionMap,
        hasImplementation = true,
        makeFinal = !isErasedSignature
    )

    result.add(wrappedMethod)
    return !isErasedSignature
}

/**
 * Alternative version of generateJavaCollectionMethodStubsIfNeeded using KaSymbol abstractions.
 *
 * @see generateJavaCollectionMethodStubsIfNeeded
 */
internal fun KaSession.generateJavaCollectionMethodStubsIfNeededV2(
    containingClass: SymbolLightClassForClassOrObject,
    classSymbol: KaNamedClassSymbol,
    allSupertypes: List<KaClassType>,
    result: MutableList<PsiMethod>,
) {
    if (classSymbol.classKind != KaClassKind.CLASS) return
    if (allSupertypes.any { (it.symbol as? KaClassSymbol)?.classKind != KaClassKind.INTERFACE }) return

    val closestMappedSupertype = allSupertypes.find { mapKotlinClassToJava(it.classId) != null } ?: return
    val javaClassId = mapKotlinClassToJava(closestMappedSupertype.classId) ?: return
    val kotlinCollectionSymbol = closestMappedSupertype.symbol as? KaClassSymbol ?: return
    val javaCollectionSymbol = findClass(javaClassId) ?: return

    generateJavaCollectionMethodStubsV2(
        containingClass,
        javaCollectionSymbol,
        kotlinCollectionSymbol,
        closestMappedSupertype,
        result
    )
}

@OptIn(KaExperimentalApi::class)
private fun KaSession.generateJavaCollectionMethodStubsV2(
    containingClass: SymbolLightClassForClassOrObject,
    javaCollectionSymbol: KaClassSymbol,
    kotlinCollectionSymbol: KaClassSymbol,
    kotlinCollectionType: KaClassType,
    result: MutableList<PsiMethod>,
) {
    val kotlinNames = kotlinCollectionSymbol.memberScope.callables
        .filter { it is KaNamedFunctionSymbol }
        .filter { it.origin != KaSymbolOrigin.JAVA_SOURCE && it.origin != KaSymbolOrigin.JAVA_LIBRARY }
        .mapNotNull { it.name?.asString() }
        .toSet()

    val javaMethods = javaCollectionSymbol.memberScope.callables
        .filterIsInstance<KaNamedFunctionSymbol>()
//        .filter { !it.hasModifier(KaSymbolModifier.DEFAULT) }
        .toList()

    val substitutionMap = buildSubstitutionMap(javaCollectionSymbol, kotlinCollectionType)

    val candidateMethods = javaMethods.flatMap { method ->
        createWrappersForJavaCollectionMethodV2(
            containingClass,
            method,
            javaCollectionSymbol,
            kotlinCollectionSymbol,
            kotlinNames,
            substitutionMap
        )
    }

    val existingSignatures = result.map { it.getSignature(PsiSubstitutor.EMPTY) }.toSet()

    result += candidateMethods.filter { candidateMethod ->
        candidateMethod.getSignature(PsiSubstitutor.EMPTY) !in existingSignatures
    }
}

private fun KaSession.createWrappersForJavaCollectionMethodV2(
    containingClass: SymbolLightClassForClassOrObject,
    method: KaNamedFunctionSymbol,
    javaCollectionSymbol: KaClassSymbol,
    kotlinCollectionSymbol: KaClassSymbol,
    kotlinNames: Set<String>,
    substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>>,
): List<PsiMethod> {
    val methodName = method.name.asString()

    val kotlinGetterNameWithDifferentAbi = JAVA_GETTER_NAME_TO_KOTLIN_GETTER_NAME_WITH_DIFFERENT_ABI[methodName]
    val hasCorrespondingKotlinDeclaration = methodName in kotlinNames || methodName in JAVA_MAP_ENTRY_METHODS
    val isSpecialNotErasedSignature = methodName in MEMBERS_WITH_SPECIAL_SIGNATURE && methodName !in ERASED_COLLECTION_METHOD_NAMES

    return when {
        kotlinGetterNameWithDifferentAbi != null -> {
            val hasImplementation = methodName == "size" && containingClass.withClassSymbol { classSymbol ->
                classSymbol.delegatedMemberScope.callables(Name.identifier("size")).toList().isNotEmpty()
            }
            val finalBridgeForJava = method.finalBridge(containingClass, substitutionMap)
            val abstractKotlinGetter = method.wrapAsSymbolMethod(
                containingClass = containingClass,
                name = kotlinGetterNameWithDifferentAbi,
                substitutionMap = substitutionMap,
                hasImplementation = hasImplementation
            )

            listOf(finalBridgeForJava, abstractKotlinGetter)
        }

        hasCorrespondingKotlinDeclaration -> {
            if (isSpecialNotErasedSignature) {
                createMethodsWithSpecialSignatureV2(
                    containingClass,
                    method,
                    javaCollectionSymbol,
                    kotlinCollectionSymbol,
                    substitutionMap
                )
            } else {
                emptyList()
            }
        }

        else -> {
            val stubOverrideOfJavaOnlyMethod = method.openBridge(containingClass, substitutionMap)
            listOf(stubOverrideOfJavaOnlyMethod)
        }
    }
}

private fun KaSession.createMethodsWithSpecialSignatureV2(
    containingClass: SymbolLightClassForClassOrObject,
    method: KaNamedFunctionSymbol,
    javaCollectionSymbol: KaClassSymbol,
    kotlinCollectionSymbol: KaClassSymbol,
    substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>>,
): List<PsiMethod> {
    val methodName = method.name.asString()

    // Case 1: two type parameters (Map methods)
    if (javaCollectionSymbol.classId?.asFqNameString() == CommonClassNames.JAVA_UTIL_MAP) {
        val abstractKotlinVariantWithGeneric = createJavaUtilMapMethodWithSpecialSignatureV2(
            containingClass,
            method,
            kotlinCollectionSymbol
        ) ?: return emptyList()
        val finalBridgeWithObject = method.finalBridge(containingClass, substitutionMap)
        return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
    }

    // Remaining cases: one type parameter
    if (methodName == "remove") {
        val singleParam = method.valueParameters.singleOrNull()
        if (singleParam != null && singleParam.returnType.isIntType) {
            // remove(int) -> final bridge remove(int), abstract removeAt(int)
            return listOf(
                method.finalBridge(containingClass, substitutionMap),
                method.wrapAsSymbolMethod(containingClass, name = "removeAt", substitutionMap = substitutionMap)
            )
        } else if (javaCollectionSymbol.classId?.asFqNameString() == CommonClassNames.JAVA_UTIL_ITERATOR) {
            // skip default method java.util.Iterator#remove()
            return emptyList()
        }
    }

    val typePointer = substitutionMap.values.singleOrNull() ?: return emptyList()
    if (typePointer.restore() is KaTypeParameterType) {
        return emptyList()
    }

    val finalBridgeWithObject = method.finalBridge(containingClass, substitutionMap)
    val abstractKotlinVariantWithGeneric = method.wrapAsSymbolMethod(
        containingClass = containingClass,
        substitutionMap = substitutionMap,
        substituteObjectWith = typePointer
    )
    return listOf(finalBridgeWithObject, abstractKotlinVariantWithGeneric)
}

private fun KaSession.createJavaUtilMapMethodWithSpecialSignatureV2(
    containingClass: SymbolLightClassForClassOrObject,
    method: KaNamedFunctionSymbol,
    kotlinCollectionSymbol: KaClassSymbol,
): SymbolLightMethodForMappedClassV2? {
    return null

    // Extract K and V from the collection type
//    val collectionType = kotlinCollectionSymbol.buildSelfClassType()
//    if (collectionType.typeArguments.size != 2) return null
//
//    val kType = collectionType.typeArguments[0].type ?: return null
//    val vType = collectionType.typeArguments[1].type ?: return null
//
//    if (kType is KaTypeParameterType || vType is KaTypeParameterType) return null
//
//    val methodName = method.name.asString()
//    val signature = when (methodName) {
//        "get" -> {
//            val paramType = kType.asPsiType(containingClass, allowErrorTypes = true) ?: return null
//            val returnType = vType.asPsiType(containingClass, allowErrorTypes = true) ?: return null
//            KaMethodSignature(parameterTypes = listOf(paramType), returnType = returnType)
//        }
//
//        "containsKey" -> {
//            val paramType = kType.asPsiType(containingClass, allowErrorTypes = true) ?: return null
//            KaMethodSignature(parameterTypes = listOf(paramType), returnType = PsiTypes.booleanType())
//        }
//
//        "containsValue" -> {
//            val paramType = vType.asPsiType(containingClass, allowErrorTypes = true) ?: return null
//            KaMethodSignature(parameterTypes = listOf(paramType), returnType = PsiTypes.booleanType())
//        }
//
//        "remove" -> {
//            // only `remove(Object)` pair (i.e. `remove(K)`) is needed
//            if (method.valueParameters.size != 1) return null
//            val paramType = kType.asPsiType(containingClass, allowErrorTypes = true) ?: return null
//            val returnType = vType.asPsiType(containingClass, allowErrorTypes = true) ?: return null
//            KaMethodSignature(parameterTypes = listOf(paramType), returnType = returnType)
//        }
//
//        else -> null
//    } ?: return null
//
//    return method.wrapAsSymbolMethod(
//        containingClass = containingClass,
//        objectSubstitution = null,
//        providedSignature = signature
//    )
}

/**
 * Builds a substitution map from Java collection type parameters to Kotlin collection type arguments.
 *
 * For example, if we have:
 * - Java class: `java.util.Collection<E>`
 * - Kotlin supertype: `kotlin.collections.Collection<String>`
 *
 * This function returns: `{ E -> String }`
 *
 * @param javaClassSymbol The Java collection class (e.g., java.util.Collection)
 * @param kotlinCollectionType The Kotlin collection supertype with concrete type arguments (e.g., Collection<String>)
 * @return A map from Java type parameter symbol pointers to Kotlin type pointers
 */
private fun buildSubstitutionMap(
    javaClassSymbol: KaClassSymbol,
    kotlinCollectionType: KaClassType,
): Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>> {
    val javaTypeParameters = javaClassSymbol.typeParameters
    val kotlinTypeArguments = kotlinCollectionType.typeArguments.mapNotNull { it.type }

    if (javaTypeParameters.size != kotlinTypeArguments.size) {
        return emptyMap()
    }

    return buildMap {
        javaTypeParameters.zip(kotlinTypeArguments).forEach { (typeParameter, typeArgument) ->
            put(typeParameter.createPointer(), typeArgument.createPointer())
        }
    }
}

private fun KaSession.tryToMapKotlinCollectionMethodToJavaMethodSymbol(
    kotlinCollectionFunction: KaNamedFunctionSymbol,
    allSupertypes: List<KaClassType>,
): KaNamedFunctionSymbol? {
    val name = kotlinCollectionFunction.name.asString()
    if (name == "addAll") {
        // "addAll" has two overloads from different types with different number of parameters
        return if (kotlinCollectionFunction.valueParameters.size == 1) {
            getJavaCollectionSymbol(allSupertypes)?.memberScope?.callables(kotlinCollectionFunction.name)
                ?.filterIsInstance<KaNamedFunctionSymbol>()
                ?.firstOrNull()
        } else {
            getJavaListSymbol(allSupertypes)?.memberScope?.callables(kotlinCollectionFunction.name)
                ?.filterIsInstance<KaNamedFunctionSymbol>()
                ?.find { it.valueParameters.size == 2 }
        }
    }

    val javaClassSymbol = when (name) {
        "contains", "containsAll", "removeAll", "retainAll" -> getJavaCollectionSymbol(allSupertypes)
        "indexOf", "lastIndexOf" -> getJavaListSymbol(allSupertypes)
        "get", "containsKey", "containsValue", "putAll" -> getJavaMapSymbol(allSupertypes)
        "remove" -> {
            if (kotlinCollectionFunction.callableId?.classId == StandardClassIds.MutableMap) {
                getJavaMapSymbol(allSupertypes)
            } else {
                getJavaCollectionSymbol(allSupertypes)
            }
        }
        else -> null
    }

    return javaClassSymbol?.memberScope?.callables(kotlinCollectionFunction.name)
        ?.filterIsInstance<KaNamedFunctionSymbol>()
        ?.firstOrNull()
}

private fun KaSession.getJavaCollectionSymbol(allSupertypes: List<KaClassType>): KaNamedClassSymbol? =
    getJavaSymbol(allSupertypes, kotlinClassId = StandardClassIds.Collection)

private fun KaSession.getJavaListSymbol(allSupertypes: List<KaClassType>): KaNamedClassSymbol? =
    getJavaSymbol(allSupertypes, kotlinClassId = StandardClassIds.List)

private fun KaSession.getJavaMapSymbol(allSupertypes: List<KaClassType>): KaNamedClassSymbol? =
    getJavaSymbol(allSupertypes, kotlinClassId = StandardClassIds.Map)

private fun KaSession.getJavaSymbol(allSupertypes: List<KaClassType>, kotlinClassId: ClassId): KaNamedClassSymbol? {
    val kotlinCollection = allSupertypes.find { it.classId == kotlinClassId } ?: return null
    val javaClassId = mapKotlinClassToJava(kotlinCollection.classId) ?: return null
    return findClass(javaClassId) as? KaNamedClassSymbol
}

private fun mapKotlinClassToJava(classId: ClassId): ClassId? {
    return JavaToKotlinClassMap.mutabilityMappings.find {
        classId == it.kotlinReadOnly || classId == it.kotlinMutable
    }?.javaClass
}

private fun KaNamedFunctionSymbol.finalBridge(
    containingClass: SymbolLightClassForClassOrObject,
    substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>>,
): SymbolLightMethodForMappedClassV2 = wrapAsSymbolMethod(
    containingClass = containingClass,
    substitutionMap = substitutionMap,
    makeFinal = true,
    hasImplementation = true
)

private fun KaNamedFunctionSymbol.openBridge(
    containingClass: SymbolLightClassForClassOrObject,
    substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>>,
): SymbolLightMethodForMappedClassV2 = wrapAsSymbolMethod(
    containingClass = containingClass,
    substitutionMap = substitutionMap,
    makeFinal = false,
    hasImplementation = true
)

private fun KaNamedFunctionSymbol.wrapAsSymbolMethod(
    containingClass: SymbolLightClassForClassOrObject,
    lightMemberOrigin: LightMemberOrigin? = null,
    name: String = this.name.asString(),
    substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>> = emptyMap(),
    substituteObjectWith: KaTypePointer<KaType>? = null,
    providedSignature: KaMethodSignature? = null,
    makeFinal: Boolean = false,
    hasImplementation: Boolean = false,
): SymbolLightMethodForMappedClassV2 {
    return SymbolLightMethodForMappedClassV2(
        functionSymbol = this,
        lightMemberOrigin = lightMemberOrigin,
        containingClass = containingClass,
        name = name,
        isFinal = makeFinal,
        hasImplementation = hasImplementation,
        substitutionMap = substitutionMap,
        substituteObjectWith = substituteObjectWith,
        providedSignature = providedSignature
    )
}

private fun KaSession.findOverriddenCollectionSymbol(symbol: KaNamedFunctionSymbol): KaNamedFunctionSymbol? =
    symbol.allOverriddenSymbols.find { it.isFromKotlinCollectionsPackage() } as? KaNamedFunctionSymbol
