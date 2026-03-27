/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

private fun ClassId.toConeFlexibleType(
    typeArguments: Array<out ConeTypeProjection>,
    typeArgumentsForUpper: Array<out ConeTypeProjection>,
    attributes: ConeAttributes
) = toLookupTag().run {
    ConeFlexibleType(
        constructClassType(typeArguments, isMarkedNullable = false, attributes),
        constructClassType(typeArgumentsForUpper, isMarkedNullable = true, attributes),
        false,
    )
}

enum class FirJavaTypeConversionMode {
    DEFAULT, ANNOTATION_MEMBER, ANNOTATION_CONSTRUCTOR_PARAMETER, SUPERTYPE,
    TYPE_PARAMETER_BOUND_FIRST_ROUND, TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND;

    val insideAnnotation: Boolean get() = this == ANNOTATION_MEMBER || this == ANNOTATION_CONSTRUCTOR_PARAMETER
}

fun FirTypeRef.resolveIfJavaType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    source: KtSourceElement?,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): FirTypeRef = when (this) {
    is FirResolvedTypeRef -> this
    is FirJavaTypeRef -> type.toFirResolvedTypeRef(session, javaTypeParameterStack, source, mode)
    else -> this
}

internal fun FirTypeRef.toConeKotlinTypeProbablyFlexible(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    source: KtSourceElement?,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): ConeKotlinType =
    (resolveIfJavaType(session, javaTypeParameterStack, source, mode) as? FirResolvedTypeRef)?.coneType
        ?: ConeErrorType(ConeSimpleDiagnostic("Type reference in Java not resolved: ${this::class.java}", DiagnosticKind.Java))

internal fun JavaType.toFirJavaTypeRef(session: FirSession, source: KtSourceElement?): FirJavaTypeRef = buildJavaTypeRef {
    annotationBuilder = {
        // Filter to only TYPE_USE annotations using the callback-based approach
        val typeUseAnnotations = filterTypeUseAnnotations { fqName -> isTypeUseAnnotationClass(fqName, session) }
        typeUseAnnotations.convertAnnotationsToFir(session, source)
    }
    type = this@toFirJavaTypeRef
    this.source = source
}

internal fun JavaType?.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    source: KtSourceElement?,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): FirResolvedTypeRef {
    return buildResolvedTypeRef {
        coneType = toConeKotlinType(session, javaTypeParameterStack, mode, source)
            .let { if (mode == FirJavaTypeConversionMode.SUPERTYPE) it.lowerBoundIfFlexible() else it }
        annotations += coneType.typeAnnotations
        this.source = source
    }
}

private fun JavaType?.toConeKotlinType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode, source: KtSourceElement?,
    additionalAnnotations: Collection<JavaAnnotation>? = null
): ConeKotlinType =
    toConeTypeProjection(session, javaTypeParameterStack, Variance.INVARIANT, mode, source, additionalAnnotations).type
        ?: ConeFlexibleType(session.builtinTypes.anyType.coneType, session.builtinTypes.nullableAnyType.coneType, isTrivial = true)

private fun JavaType?.toConeTypeProjection(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    parameterVariance: Variance, mode: FirJavaTypeConversionMode,
    source: KtSourceElement?,
    additionalAnnotations: Collection<JavaAnnotation>? = null
): ConeTypeProjection {
    val attributes = if (this != null && (annotations.isNotEmpty() || additionalAnnotations != null)) {
        // Filter to only include TYPE_USE annotations using the callback-based approach.
        // This allows java-direct to resolve annotation classes via FIR's symbol provider.
        val isTypeUseAnnotation: (String) -> Boolean = { fqName ->
            isTypeUseAnnotationClass(fqName, session)
        }
        
        val typeUseAnnotations = filterTypeUseAnnotations(isTypeUseAnnotation)
        
        val additionalTypeUseAnnotations = additionalAnnotations?.filter { annotation ->
            val fqName = annotation.classId?.asSingleFqName()?.asString() ?: return@filter false
            isTypeUseAnnotation(fqName)
        }
        
        val convertedAnnotations = buildList {
            if (typeUseAnnotations.isNotEmpty()) {
                addAll(typeUseAnnotations.convertAnnotationsToFir(session, source))
            }

            if (additionalTypeUseAnnotations != null && additionalTypeUseAnnotations.isNotEmpty()) {
                addAll(additionalTypeUseAnnotations.convertAnnotationsToFir(session, source))
            }
        }

        if (convertedAnnotations.isNotEmpty()) {
            ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(convertedAnnotations)))
        } else {
            ConeAttributes.Empty
        }
    } else {
        ConeAttributes.Empty
    }

    return when (this) {
        is JavaClassifierType -> {
            val lowerBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes, source)
            if (mode.insideAnnotation) {
                return lowerBound
            }
            
            // Detect raw types for external classes (classifier == null).
            // A type is raw if no type arguments are provided but the class has type parameters.
            // During TYPE_PARAMETER_BOUND_FIRST_ROUND, skip raw type detection to avoid triggering
            // enhancement cycles with cyclic type bounds (e.g., class A<T extends B> and class B<T extends A>).
            val isRawType = isRaw || run {
                if (classifier != null || typeArguments.isNotEmpty()) false
                else if (mode == FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND) false
                else {
                    // For unresolved types (star imports, java.lang), classifierQualifiedName may be
                    // a simple name like "List". Use resolve callback to get FQN first.
                    val resolvedClassId = resolveTypeName(classifierQualifiedName, this, session, source)
                    val mappedClassId = JavaToKotlinClassMap.mapJavaToKotlin(resolvedClassId.asSingleFqName()) ?: resolvedClassId
                    val hasTypeParams = mappedClassId.toLookupTag().toRegularClassSymbol(session)?.typeParameterSymbols?.isNotEmpty() == true
                    // Don't treat as raw if outer type args can be inferred from the hierarchy
                    // (inherited inner class types like NestedInSuperClass resolved to SuperClass.NestedInSuperClass)
                    if (hasTypeParams && resolvedClassId.relativeClassName.pathSegments().size > 1 &&
                        containingClassIds.isNotEmpty() &&
                        findOuterTypeArgsFromHierarchy(resolvedClassId, containingClassIds, session) != null
                    ) false
                    else hasTypeParams
                }
            }
            
            if (!isRawType && classifier?.isTriviallyFlexible() == true) {
                lowerBound.toTrivialFlexibleType(session.typeContext)
            } else {
                val upperBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes, source, lowerBound)

                if (isRawType) {
                    ConeRawType.create(lowerBound, upperBound)
                } else {
                    // When isTriviallyFlexibleHint is true the class is a user-defined Java source
                    // class (cross-file reference). Use isTrivial=true to match PSI rendering (T!
                    // instead of ft<T,T?>). The upper bound is still computed to preserve any symbol-
                    // loading side effects from toConeKotlinTypeForFlexibleBound.
                    ConeFlexibleType(lowerBound, upperBound, isTrivial = isTriviallyFlexibleHint)
                }
            }
        }

        is JavaArrayType -> {
            val [classId, arguments] = when (val componentType = componentType) {
                is JavaPrimitiveType ->
                    StandardClassIds.byName(componentType.type!!.arrayTypeName.identifier) to arrayOf()

                else ->
                    StandardClassIds.Array to arrayOf(componentType.toConeKotlinType(session, javaTypeParameterStack, mode, source))
            }
            val argumentsWithOutProjection = Array(arguments.size) { ConeKotlinTypeProjectionOut(arguments[it]) }
            when (mode) {
                FirJavaTypeConversionMode.ANNOTATION_CONSTRUCTOR_PARAMETER ->
                    classId.constructClassLikeType(argumentsWithOutProjection, isMarkedNullable = false, attributes)
                FirJavaTypeConversionMode.ANNOTATION_MEMBER ->
                    classId.constructClassLikeType(arguments, isMarkedNullable = false, attributes)
                else ->
                    classId.toConeFlexibleType(arguments, typeArgumentsForUpper = argumentsWithOutProjection, attributes)
            }
        }

        is JavaPrimitiveType ->
            StandardClassIds.byName(type?.typeName?.identifier ?: "Unit")
                .constructClassLikeType(attributes = attributes)

        is JavaWildcardType -> {
            // TODO: this discards annotations on wildcards, allowed since Java 8 - what do they mean?
            //    List<@NotNull ? extends @Nullable Object>
            val bound = this.bound
            val argumentVariance = if (isExtends) Variance.OUT_VARIANCE else Variance.IN_VARIANCE
            if (bound == null || (parameterVariance != Variance.INVARIANT && parameterVariance != argumentVariance)) {
                ConeStarProjection
            } else {
                val nullabilityAnnotationOnWildcard = extractNullabilityAnnotationOnBoundedWildcard(this)?.let(::listOf)
                val boundType = bound.toConeKotlinType(session, javaTypeParameterStack, mode, source, nullabilityAnnotationOnWildcard)
                if (isExtends) ConeKotlinTypeProjectionOut(boundType) else ConeKotlinTypeProjectionIn(boundType)
            }
        }

        null -> ConeStarProjection
        else -> errorWithAttachment("Strange JavaType: ${this::class.java}") {
            withEntry("type", this@toConeTypeProjection) { it.toString() }
        }
    }
}

private val javaReadOnlyFqNames = JavaToKotlinClassMap.getReadOnlyAsJava()

private fun JavaClassifier.isTriviallyFlexible(): Boolean {
    return this is JavaClass && fqName !in javaReadOnlyFqNames || this is JavaTypeParameter
}

private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode,
    attributes: ConeAttributes,
    source: KtSourceElement?,
    lowerBound: ConeLookupTagBasedType? = null
): ConeLookupTagBasedType {
    fun buildTypeProjections(lookupTag: ConeClassLikeLookupTagImpl): Array<ConeTypeProjection> {
        val typeParameterSymbols =
            lookupTag.takeIf { mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND }
                ?.toRegularClassSymbol(session)?.typeParameterSymbols
        // Truncate type arguments to match the class's type parameter count when the source has
        // more arguments than the class declares (wrong-arity type references in Java source).
        val effectiveArgCount = if (typeParameterSymbols != null) minOf(typeArguments.size, typeParameterSymbols.size) else typeArguments.size
        return Array(effectiveArgCount) { index ->
            // TODO: check this
            val newMode = if (mode.insideAnnotation) FirJavaTypeConversionMode.DEFAULT else mode
            val argument = typeArguments[index]
            val variance = typeParameterSymbols?.getOrNull(index)?.fir?.variance ?: Variance.INVARIANT
            argument.toConeTypeProjection(session, javaTypeParameterStack, variance, newMode, source)
        }
    }

    return when (val classifier = classifier) {
        is JavaClass -> {
            var classId = if (mode.insideAnnotation) {
                JavaToKotlinClassMap.mapJavaToKotlinIncludingClassMapping(classifier.fqName!!)
            } else {
                JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!)
            } ?: classifier.classId!!

            if (lowerBound == null || argumentsMakeSenseOnlyForMutableContainer(classId, session)) {
                classId = classId.readOnlyToMutable() ?: classId
            }

            val lookupTag = classId.toLookupTag()
            // When converting type parameter bounds we should not attempt to load any classes, as this may trigger
            // enhancement of type parameter bounds on some other class that depends on this one. Also, in case of raw
            // types specifically there could be an infinite recursion on the type parameter itself.
            val mappedTypeArguments = when {
                isRaw -> {
                    val typeParameterSymbols =
                        lookupTag.takeIf { lowerBound == null && mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND }
                            ?.toRegularClassSymbol(session)?.typeParameterSymbols
                    // Given `C<T : X>`, `C` -> `C<X>..C<*>?`.
                    when {
                        mode.insideAnnotation -> Array(classifier.allTypeParametersNumber()) { ConeStarProjection }
                        else -> typeParameterSymbols?.getProjectionsForRawType(session, nullabilities = null)
                            ?: Array(classifier.allTypeParametersNumber()) { ConeStarProjection }
                    }
                }
                lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> buildTypeProjections(lookupTag)
                else -> lowerBound?.typeArguments
            }

            lookupTag.constructClassType(mappedTypeArguments ?: ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = lowerBound != null, attributes)
        }

        is JavaTypeParameter -> {
            val symbol = javaTypeParameterStack[classifier]
            if (symbol != null) {
                ConeTypeParameterTypeImpl(symbol.toLookupTag(), isMarkedNullable = lowerBound != null, attributes)
            } else {
                ConeErrorType(ConeUnresolvedNameError(classifier.name))
            }
        }

        null -> {
            val qualifiedName = this.classifierQualifiedName

            var classId = if (!isResolved) {
                // Resolve the name - this handles both simple names and nested class references like "Map.Entry"
                resolveTypeName(qualifiedName, this, session, source)
            } else {
                // For resolved names, try the resolve callback to correctly handle nested class FQNs.
                // ClassId.topLevel would incorrectly split "a.X.Y" as package "a.X", class "Y"
                // when the actual class is package "a", nested class "X.Y".
                // We use the resolve callback instead of findClassId because findClassId probes the
                // session's symbol provider directly, which in LL-FIR can trigger lazy resolution of
                // the very class being resolved, causing infinite recursion (stack overflow).
                // The resolve callback returns null by default (PSI types), safely falling back to
                // ClassId.topLevel. For java-direct types, it uses the resolution context to generate
                // correct ClassId candidates without triggering LL-FIR lazy resolution cycles.
                resolve(
                    tryResolve = { candidateClassId ->
                        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(candidateClassId)
                        // Reject builtins-only classes to match PSI behavior.
                        // PSI resolves classes through compiled .class files and light classes.
                        // Kotlin builtins (origin=BuiltIns) exist only in FIR's symbol provider,
                        // not as .class files, so PSI can't resolve them. When stdlib is on the
                        // classpath, these classes have origin=Library instead of BuiltIns.
                        symbol != null && symbol.origin != FirDeclarationOrigin.BuiltIns
                    },
                    getSupertypeClassIds = { classId -> getResolvedSupertypeClassIds(classId, session) },
                ) ?: ClassId.topLevel(FqName(qualifiedName))
            }

            classId = if (mode.insideAnnotation) {
                JavaToKotlinClassMap.mapJavaToKotlinIncludingClassMapping(classId.asSingleFqName())
            } else {
                JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName())
            } ?: classId

            if (lowerBound == null || argumentsMakeSenseOnlyForMutableContainer(classId, session)) {
                classId = classId.readOnlyToMutable() ?: classId
            }

            val lookupTag = classId.toLookupTag()

            // Detect raw types for external classes (classifier == null).
            // A type is raw if no type arguments are provided but the class has type parameters.
            // This can happen when java-direct parses Java code that references Kotlin or library classes.
            //
            // During TYPE_PARAMETER_BOUND_FIRST_ROUND, we must NOT load class symbols because this could
            // trigger enhancement of type parameter bounds on other classes that depend on this one,
            // causing infinite recursion with cyclic type bounds (e.g., class A<T extends B> and class B<T extends A>).
            // This matches the safety check in the JavaClass branch above (line 264).
            val typeParameterSymbols = lookupTag
                .takeIf { mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND }
                ?.toRegularClassSymbol(session)?.typeParameterSymbols

            // For inherited inner class types (e.g., NestedInSuperClass resolved to SuperClass.NestedInSuperClass),
            // the outer class type arguments are implicit and should come from the containing class's supertype chain.
            // Without this, such types would be incorrectly treated as raw types.
            val outerTypeArgs = if (
                !isRaw && typeArguments.isEmpty() &&
                typeParameterSymbols != null && typeParameterSymbols.isNotEmpty() &&
                classId.relativeClassName.pathSegments().size > 1 && // nested class
                containingClassIds.isNotEmpty()
            ) {
                findOuterTypeArgsFromHierarchy(classId, containingClassIds, session)
            } else null

            val isRawType = isRaw || (outerTypeArgs == null && typeParameterSymbols != null && typeParameterSymbols.isNotEmpty() &&
                (typeArguments.isEmpty() || typeArguments.size < typeParameterSymbols.size))

            val mappedTypeArguments = when {
                outerTypeArgs != null -> outerTypeArgs
                isRawType -> {
                    // Same handling as JavaClass branch for raw types.
                    // For lower bound (lowerBound == null), use erased upper bounds via getProjectionsForRawType.
                    // For upper bound (lowerBound != null), use star projections.
                    // In TYPE_PARAMETER_BOUND_FIRST_ROUND, always use star projections to avoid enhancement cycles.
                    when {
                        lowerBound != null -> typeParameterSymbols?.let { Array(it.size) { ConeStarProjection } }
                        mode.insideAnnotation || mode == FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND ->
                            typeParameterSymbols?.let { Array(it.size) { ConeStarProjection } }
                        else -> typeParameterSymbols?.getProjectionsForRawType(session, nullabilities = null)
                    }
                }
                lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> buildTypeProjections(lookupTag)
                else -> lowerBound?.typeArguments
            }

            lookupTag.constructClassType(mappedTypeArguments ?: ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = lowerBound != null, attributes)
        }

        else -> ConeErrorType(ConeSimpleDiagnostic("Unexpected classifier: $classifier", DiagnosticKind.Java))
    }
}

private fun resolveTypeName(
    name: String,
    javaType: JavaClassifierType,
    session: FirSession,
    source: KtSourceElement?
): ClassId {
    // Try ClassId-based resolution which avoids package/class ambiguity.
    // The callback checks if a ClassId exists in the symbol provider.
    val resolvedClassId = javaType.resolve(
        tryResolve = { candidateClassId ->
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(candidateClassId)
            symbol != null && symbol.origin != FirDeclarationOrigin.BuiltIns
        },
        getSupertypeClassIds = { classId -> getResolvedSupertypeClassIds(classId, session) },
    )
    if (resolvedClassId != null) return resolvedClassId

    // Fall back to probing different package/class boundaries for the raw name
    return findClassId(name, session) ?: ClassId.topLevel(FqName(name))
}

/**
 * Returns the direct supertype ClassIds for a given class, reading only already-resolved types.
 * Only works for non-Java classes (Kotlin/builtin) to avoid triggering premature lazy resolution
 * of Java class supertypes. Kotlin class supertypes are resolved in the SUPER_TYPES phase
 * which runs before Java class member conversion.
 */
private fun getResolvedSupertypeClassIds(classId: ClassId, session: FirSession): List<ClassId> {
    val firClass = classId.toLookupTag().toRegularClassSymbol(session)?.fir ?: return emptyList()
    // Only read supertypes from non-Java classes. Java class supertypes are walked
    // via the class finder in Phase 1 of resolveInheritedInnerClassToClassId.
    // Accessing FirJavaClass.superTypeRefs here could trigger premature lazy resolution.
    if (firClass is FirJavaClass) return emptyList()
    return firClass.superTypeRefs.mapNotNull { ref ->
        (ref as? FirResolvedTypeRef)?.coneType?.classId
    }
}

/**
 * Finds the outer class type arguments for an inherited inner class type by walking
 * the containing class hierarchy's FIR supertypes.
 *
 * For example, for `NestedInSuperClass` in `J1.NestedSubClass extends NestedInSuperClass`,
 * where `J1 → KFirst → SuperClass<String>`, this finds `SuperClass<String>` and returns `[String]`.
 *
 * @param classId the resolved ClassId of the inner class (e.g., SuperClass.NestedInSuperClass)
 * @param containingClassIds ClassIds from innermost containing class to outermost
 * @param session the FIR session
 * @return the outer type arguments, or null if they can't be determined
 */
private fun findOuterTypeArgsFromHierarchy(
    classId: ClassId,
    containingClassIds: List<ClassId>,
    session: FirSession,
): Array<out ConeTypeProjection>? {
    val outerClassId = classId.outerClassId ?: return null
    // Skip the first containing class (index 0) — it's the class whose supertypes are currently
    // being resolved. Accessing its superTypeRefs would cause infinite recursion.
    // Start from outer classes (index 1+), which have their supertypes already resolved
    // because FIR resolves outer class supertypes before inner class supertypes.
    for (i in 1 until containingClassIds.size) {
        val containingId = containingClassIds[i]
        val containingFir = containingId.toLookupTag().toRegularClassSymbol(session)?.fir ?: continue
        for (superRef in containingFir.superTypeRefs) {
            val superType = (superRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
            val result = findTypeArgsForClassInHierarchy(superType, outerClassId, session, mutableSetOf())
            if (result != null) return result
        }
    }
    return null
}

/**
 * Recursively searches for a target class in a type's supertype hierarchy and returns
 * its type arguments. Only walks through non-Java classes to avoid premature resolution.
 *
 * @param type the current supertype being examined
 * @param targetClassId the outer class ClassId we're looking for
 * @param session the FIR session
 * @param visited set of already-visited ClassIds to avoid cycles
 * @return the type arguments for the target class, or null if not found
 */
private fun findTypeArgsForClassInHierarchy(
    type: ConeClassLikeType,
    targetClassId: ClassId,
    session: FirSession,
    visited: MutableSet<ClassId>,
): Array<out ConeTypeProjection>? {
    val typeClassId = type.lookupTag.classId
    if (typeClassId == targetClassId) return type.typeArguments
    if (!visited.add(typeClassId)) return null

    val firClass = typeClassId.toLookupTag().toRegularClassSymbol(session)?.fir ?: return null
    // Only walk through non-Java classes to avoid premature lazy resolution.
    if (firClass is FirJavaClass) return null

    for (superRef in firClass.superTypeRefs) {
        val superType = (superRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
        // Substitute type arguments when walking through intermediate classes.
        // E.g., class A<X> : SuperClass<X>, A<String> → SuperClass<String>
        val substitutedType = substituteTypeArgs(superType, type, firClass, session)
        val result = findTypeArgsForClassInHierarchy(substitutedType, targetClassId, session, visited)
        if (result != null) return result
    }
    return null
}

/**
 * Substitutes type parameter references in a declared supertype with concrete type arguments
 * from the actual type. E.g., if `A<X> : SuperClass<X>` and the actual type is `A<String>`,
 * substitutes `SuperClass<X>` to `SuperClass<String>`.
 */
private fun substituteTypeArgs(
    declaredSupertype: ConeClassLikeType,
    actualType: ConeClassLikeType,
    declaringClass: FirRegularClass,
    @Suppress("UNUSED_PARAMETER") session: FirSession,
): ConeClassLikeType {
    if (actualType.typeArguments.isEmpty() || declaringClass.typeParameters.isEmpty()) return declaredSupertype
    // Build substitution map: type parameter -> actual type argument
    val substitutionMap = mutableMapOf<ConeTypeParameterLookupTag, ConeTypeProjection>()
    for ((index, typeParam) in declaringClass.typeParameters.withIndex()) {
        if (index < actualType.typeArguments.size) {
            substitutionMap[typeParam.symbol.toLookupTag()] = actualType.typeArguments[index]
        }
    }
    if (substitutionMap.isEmpty()) return declaredSupertype
    // Substitute type arguments in the declared supertype
    val newArgs = Array(declaredSupertype.typeArguments.size) { index ->
        val arg = declaredSupertype.typeArguments[index]
        when (arg) {
            is ConeTypeParameterType -> substitutionMap[arg.lookupTag] ?: arg
            else -> arg
        }
    }
    return declaredSupertype.lookupTag.constructClassType(newArgs, declaredSupertype.isMarkedNullable)
}

/**
 * Finds the correct ClassId for a fully qualified name that may include nested classes.
 * For example, "java.util.Map.Entry" becomes ClassId(FqName("java.util"), FqName("Map.Entry")).
 *
 * This function probes different package/class boundaries using the symbol provider
 * to find which split actually resolves to a class.
 */
internal fun findClassId(fqn: String, session: FirSession): ClassId? {
    val parts = fqn.split('.')
    if (parts.isEmpty()) return null

    // Try progressively longer class names (shorter package prefixes)
    // For "java.util.Map.Entry", try:
    //   1. ClassId("java.util.Map", "Entry")
    //   2. ClassId("java.util", "Map.Entry")
    //   3. ClassId("java", "util.Map.Entry")
    //   4. ClassId("", "java.util.Map.Entry")
    for (classStartIndex in (parts.size - 1) downTo 0) {
        val packageFqName = if (classStartIndex == 0) {
            FqName.ROOT
        } else {
            FqName.fromSegments(parts.subList(0, classStartIndex))
        }
        val relativeClassName = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
        val classId = ClassId(packageFqName, relativeClassName, isLocal = false)

        if (session.symbolProvider.getClassLikeSymbolByClassId(classId) != null) {
            return classId
        }
    }

    return null
}

private fun JavaClass.allTypeParametersNumber(): Int {
    var current: JavaClass? = this
    var result = 0
    while (current != null) {
        result += current.typeParameters.size
        current = if (current.isStatic) null else current.outerClass
    }
    return result
}

// Returns true for covariant read-only container that has mutable pair with invariant parameter
// List<in A> does not make sense, but MutableList<in A> does
// Same for Map<K, in V>
// But both Iterable<in A>, MutableIterable<in A> don't make sense as they are covariant, so return false
private fun JavaClassifierType.argumentsMakeSenseOnlyForMutableContainer(
    classId: ClassId,
    session: FirSession,
): Boolean {
    if (!JavaToKotlinClassMap.isReadOnly(classId.asSingleFqName().toUnsafe())) return false
    val mutableClassId = classId.readOnlyToMutable() ?: return false

    if (!typeArguments.lastOrNull().isSuperWildcard()) return false
    val mutableLastParameterVariance =
        mutableClassId.toLookupTag().toRegularClassSymbol(session)?.typeParameterSymbols?.lastOrNull()?.variance
            ?: return false

    return mutableLastParameterVariance != Variance.OUT_VARIANCE
}

/**
 * Checks if an annotation class has TYPE_USE in its @Target annotation.
 * 
 * This is used to filter type annotations - only annotations with @Target(ElementType.TYPE_USE)
 * should appear on types. This matches the behavior of javac-wrapper's filterTypeAnnotations().
 * 
 * @param fqName fully qualified name of the annotation class
 * @param session FIR session for resolving the annotation class
 * @return true if the annotation has TYPE_USE target, false otherwise
 */
private fun isTypeUseAnnotationClass(fqName: String, session: FirSession): Boolean {
    val classId = findClassId(fqName, session) ?: ClassId.topLevel(FqName(fqName))
    
    // Resolve the annotation class
    val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)
    
    // Verify the symbol's classId matches our request. Some class finders may return
    // classes from different packages when the requested package doesn't contain the class.
    // This happens with PSI-based finders that match by simple name alone.
    if (symbol != null && symbol.classId != classId) {
        return false
    }
    
    val annotationClass = symbol?.fir as? FirRegularClass ?: return false
    
    // Find @Target annotation on the annotation class
    // It could be java.lang.annotation.Target or kotlin.annotation.Target (mapped)
    val targetAnnotation = annotationClass.annotations.find { firAnnotation ->
        val targetClassId = firAnnotation.annotationTypeRef.coneType.classId
        targetClassId == JvmStandardClassIds.Annotations.Java.Target ||
                targetClassId == StandardClassIds.Annotations.Target
    } ?: return false
    
    // Check if TYPE_USE is in the target values
    return hasTypeUseTarget(targetAnnotation)
}

/**
 * Checks if a @Target annotation contains TYPE_USE (or TYPE for Kotlin's @Target).
 */
private fun hasTypeUseTarget(targetAnnotation: FirAnnotation): Boolean {
    val argumentMapping = targetAnnotation.argumentMapping.mapping
    if (argumentMapping.isEmpty()) return false
    
    // The argument could be named "value" (Java) or "allowedTargets" (Kotlin)
    val argument = argumentMapping.values.firstOrNull() ?: return false
    
    return when (argument) {
        is FirVarargArgumentsExpression -> argument.arguments.any { isTypeUseElement(it) }
        is FirCollectionLiteral -> argument.argumentList.arguments.any { isTypeUseElement(it) }
        else -> isTypeUseElement(argument)
    }
}

/**
 * Checks if an expression represents ElementType.TYPE_USE (Java) or AnnotationTarget.TYPE (Kotlin).
 */
private fun isTypeUseElement(expr: FirExpression): Boolean {
    return when (expr) {
        is FirEnumEntryDeserializedAccessExpression -> {
            // For Java: ElementType.TYPE_USE
            // For Kotlin: AnnotationTarget.TYPE
            val entryName = expr.enumEntryName.asString()
            entryName == "TYPE_USE" || entryName == "TYPE"
        }
        is FirPropertyAccessExpression -> {
            val calleeReference = expr.calleeReference
            if (calleeReference is FirResolvedNamedReference) {
                val name = calleeReference.name.asString()
                name == "TYPE_USE" || name == "TYPE"
            } else false
        }
        else -> false
    }
}
