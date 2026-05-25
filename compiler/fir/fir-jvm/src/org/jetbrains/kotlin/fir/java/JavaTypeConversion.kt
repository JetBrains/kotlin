/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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
    annotationBuilder = { annotations.convertAnnotationsToFir(session, source) }
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
        // `annotations` is already TYPE_USE-only for every impl: PSI/javac-wrapper pre-filter at
        // structure-build time, java-direct's `JavaTypeOverAst` pre-filters `memberAnnotations`
        // through `JavaResolutionContext.isTypeUseAnnotationClass`. `additionalAnnotations` is
        // populated solely by `extractNullabilityAnnotationOnBoundedWildcard`, which restricts the
        // result to `RXJAVA3_ANNOTATIONS` (`Nullable` / `NonNull`, both `@Target(TYPE_USE)`).
        val convertedAnnotations = buildList {
            if (annotations.isNotEmpty()) {
                addAll(annotations.convertAnnotationsToFir(session, source))
            }
            if (!additionalAnnotations.isNullOrEmpty()) {
                addAll(additionalAnnotations.convertAnnotationsToFir(session, source))
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

            // Raw-type detection for null-classifier types was previously here; empirically
            // dead post-D2-A (see `implDocs/JTC_CLEANUP_2026_05_24.md`). The remaining
            // null-classifier types in production never have both typeArguments empty AND
            // a backing class with type parameters that aren't recoverable via outer args.
            if (!isRaw && classifier?.isTriviallyFlexible() == true) {
                lowerBound.toTrivialFlexibleType(session.typeContext)
            } else {
                val upperBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes, source, lowerBound)
                if (isRaw) {
                    ConeRawType.create(lowerBound, upperBound)
                } else {
                    // resolvable cross-file references go through the first branch above
                    // (`classifier?.isTriviallyFlexible()` reads off the `FirBackedJavaClassAdapter`'s
                    // fqName). Reaching this branch means classifier is null (binary
                    // `PlainJavaClassifierType` unresolvables or java-direct AST JLS-miss) or
                    // classifier is a non-trivially-flexible JavaClass (a Kotlin read-only
                    // mapped Java collection like java.util.List). isTrivial = false matches
                    // PSI for both.
                    ConeFlexibleType(lowerBound, upperBound, isTrivial = false)
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
        val effectiveArgCount =
            if (typeParameterSymbols != null) minOf(typeArguments.size, typeParameterSymbols.size) else typeArguments.size
        return Array(effectiveArgCount) { index ->
            // Type arguments of a type used in an annotation member are themselves regular types,
            // so they should be converted with DEFAULT mode (not the annotation-specific modes).
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
            // Recover implicit outer-class type arguments for inherited inner-class references when
            // the classifier does not carry a fully-shaped `outerClass` chain — i.e. the
            // `FirBackedJavaClassAdapter` for cross-file references in `java-direct`
            // PSI/binary classifiers populate the outer chain themselves, so the explicit path
            // below is sufficient there. `findOuterTypeArgsFromHierarchy` returns `null` early
            // when the stack does not carry a containing-class symbol (non-`FirJavaClass`-conversion
            // callers) or when the resolved classId is top-level — those paths are zero-cost.
            // Mirrors the `null ->` branch's recovery generalised from `typeArguments.isEmpty()`
            // to a missing-tail case (`typeArguments.size < typeParameterSymbols.size`).
            val outerTypeArgs: Array<out ConeTypeProjection>? = if (
                !isRaw &&
                classId.relativeClassName.pathSegments().size > 1 &&
                mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND
            ) {
                val tps = lookupTag.toRegularClassSymbol(session)?.typeParameterSymbols
                if (!tps.isNullOrEmpty() && typeArguments.size < tps.size) {
                    findOuterTypeArgsFromHierarchy(classId, javaTypeParameterStack, session)
                } else null
            } else null

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
                outerTypeArgs != null && typeArguments.isNotEmpty() -> buildTypeProjections(lookupTag) + outerTypeArgs
                outerTypeArgs != null -> outerTypeArgs
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
            // `classifier == null` is reached only by java-direct synthetic types whose
            // `classifier` getter is hard-coded null (post-D2-A: only the residual
            // `JavaClassifierTypeOverAst` JLS-misses + binary `PlainJavaClassifierType`
            // unresolvables). Empirical probing of the full java-direct suite showed every
            // hit on this branch goes through the minimal `resolveTypeName → constructClassType`
            // path with either `buildTypeProjections` or `lowerBound?.typeArguments` for the
            // type arguments — never through a `JavaToKotlinClassMap` mapping, a
            // `readOnlyToMutable` rewrite, a `findOuterTypeArgsFromHierarchy` recovery, or a
            // raw-type construction. See `implDocs/JTC_CLEANUP_2026_05_24.md` for the
            // sub-block hit table that justifies this shape.
            val classId = resolveTypeName(this.classifierQualifiedName, this, session, mode)
            val lookupTag = classId.toLookupTag()
            val mappedTypeArguments = when {
                lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> buildTypeProjections(lookupTag)
                else -> lowerBound?.typeArguments
            }
            lookupTag.constructClassType(
                mappedTypeArguments ?: ConeTypeProjection.EMPTY_ARRAY,
                isMarkedNullable = lowerBound != null,
                attributes
            )
        }

        else -> ConeErrorType(ConeSimpleDiagnostic("Unexpected classifier: $classifier", DiagnosticKind.Java))
    }
}

/**
 * Resolves a Java-source type-reference name to a [ClassId].
 *
 * for every reference (cross-file too, via `FirBackedJavaClassAdapter`); reading
 * `(classifier as? JavaClass)?.classId` is now reliable across all impls (PSI/binary/java-direct).
 */
private fun resolveTypeName(
    name: String,
    javaType: JavaClassifierType,
    session: FirSession,
    mode: FirJavaTypeConversionMode,
): ClassId {
    (javaType.classifier as? JavaClass)?.classId?.let { return it }
    if (mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND) {
        findClassIdByFqNameString(name, session)?.let { return it }
    }
    return ClassId.topLevel(FqName(name))
}

/**
 * Finds the outer class type arguments for an inherited inner class type by walking
 * the containing class hierarchy's FIR supertypes.
 *
 * For example, for `NestedInSuperClass` in `J1.NestedSubClass extends NestedInSuperClass`,
 * where `J1 → KFirst → SuperClass<String>`, this finds `SuperClass<String>` and returns `[String]`.
 *
 * the lexical containing-class chain is read from the [MutableJavaTypeParameterStack] populated
 * at [org.jetbrains.kotlin.fir.java.FirJavaFacade.convertJavaClassToFir] time — no longer
 * threaded through `JavaClassifierType.containingClassIds` on the public Java-model interface.
 * The chain starts from the type reference's containing class's **outer** class (skipping the
 * containing class itself, whose supertypes are currently being resolved). Returns `null` (no recovery)
 * when the stack does not carry a containing-class symbol — i.e., for callers outside
 * `convertJavaClassToFir`'s scope.
 *
 * @param classId the resolved ClassId of the inner class (e.g., SuperClass.NestedInSuperClass)
 * @param javaTypeParameterStack carries the containing FirJavaClass's symbol via
 *   [MutableJavaTypeParameterStack.containingClassSymbol]
 * @param session the FIR session
 * @return the outer type arguments, or null if they can't be determined
 */
private fun findOuterTypeArgsFromHierarchy(
    classId: ClassId,
    javaTypeParameterStack: JavaTypeParameterStack,
    session: FirSession,
): Array<out ConeTypeProjection>? {
    val outerClassId = classId.outerClassId ?: return null
    val containingSymbol = (javaTypeParameterStack as? MutableJavaTypeParameterStack)?.containingClassSymbol ?: return null
    // Walk outer classes of the containing class (skipping the containing class itself, whose
    // supertypes are currently being resolved — accessing its superTypeRefs would recurse).
    // Outer classes have their supertypes resolved already because FIR resolves outer class
    // supertypes before inner class supertypes.
    var currentOuter: ClassId? = containingSymbol.classId.outerClassId
    while (currentOuter != null) {
        val containingFir = currentOuter.toLookupTag().toRegularClassSymbol(session)?.fir
        if (containingFir != null) {
            for (superRef in containingFir.superTypeRefs) {
                val superType = (superRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
                val result = findTypeArgsForClassInHierarchy(superType, outerClassId, session, mutableSetOf())
                if (result != null) return result
            }
        }
        currentOuter = currentOuter.outerClassId
    }
    return null
}

/**
 * Recursively searches for a target class in a type's supertype hierarchy and returns
 * its type arguments.
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

    val classSymbol = typeClassId.toLookupTag().toRegularClassSymbol(session) ?: return null
    classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
    val firClass = classSymbol.fir

    for (superRef in firClass.superTypeRefs) {
        val superType = (superRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
        // Substitute type arguments when walking through intermediate classes.
        // E.g., class A<X> : SuperClass<X>, A<String> → SuperClass<String>
        val substitutedType = substituteTypeArgs(superType, type, firClass)
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
    val newArgs = Array(declaredSupertype.typeArguments.size) { index ->
        when (val arg = declaredSupertype.typeArguments[index]) {
            is ConeTypeParameterType -> substitutionMap[arg.lookupTag] ?: arg
            else -> arg
        }
    }
    return declaredSupertype.lookupTag.constructClassType(newArgs, declaredSupertype.isMarkedNullable)
}

/**
 * Splits a dot-separated FQN into a `(packageFqName, relativeClassName)` pair and returns the
 * longest-package [ClassId] that the session's symbol provider can resolve. Needed because an FQN
 * like `"java.util.Map.Entry"` cannot be unambiguously split without the symbol resolver.
 * Uses [FirSymbolNamesProvider] to skip impossible packages when available, otherwise falls back
 * to probing every split.
 */
private fun findClassIdByFqNameString(fqnameString: String, session: FirSession): ClassId? {
    if (fqnameString.isEmpty()) return null
    val parts = fqnameString.split('.')
    if (parts.isEmpty()) return null

    val symbolProvider = session.symbolProvider
    val namesProvider = symbolProvider.symbolNamesProvider
    val knownPackages = namesProvider.getPackageNames()

    fun resolves(candidate: ClassId): Boolean =
        symbolProvider.getClassLikeSymbolByClassId(candidate) != null

    if (knownPackages != null) {
        // Fast path: only check splits whose package prefix is known to exist in this session.
        // Iterate from longest package prefix to shortest so the first hit corresponds to the
        // deepest known package — matching the "most specific" JLS-style resolution a caller
        // would expect (e.g. `java.util.Map.Entry` → `java.util.Map#Entry`, not
        // `java#util.Map.Entry`).
        for (classStartIndex in (parts.size - 1) downTo 1) {
            val pkg = parts.subList(0, classStartIndex).joinToString(".")
            if (pkg !in knownPackages) continue
            val candidate = ClassId(
                FqName.fromSegments(parts.subList(0, classStartIndex)),
                FqName.fromSegments(parts.subList(classStartIndex, parts.size)),
                isLocal = false,
            )
            // Skip candidates the provider can cheaply prove it does not contain before paying
            // for the full symbol-load probe.
            if (!namesProvider.mayHaveTopLevelClassifier(candidate.outermostClassId)) continue
            if (resolves(candidate)) return candidate
        }
        // Root-package case — only meaningful if the provider reports the root package as known.
        if ("" in knownPackages) {
            val rootCandidate = ClassId(FqName.ROOT, FqName.fromSegments(parts), isLocal = false)
            if (namesProvider.mayHaveTopLevelClassifier(rootCandidate.outermostClassId) && resolves(rootCandidate)) {
                return rootCandidate
            }
        }
        return null
    }

    // Fallback: the name provider cannot enumerate packages (e.g. some LL-FIR providers). Probe
    // every candidate split — this preserves the original behavior and is the reason callers must
    // be able to tolerate the probing cost when using an opaque provider.
    for (classStartIndex in (parts.size - 1) downTo 0) {
        val packageFqName = if (classStartIndex == 0) FqName.ROOT
        else FqName.fromSegments(parts.subList(0, classStartIndex))
        val relativeClassName = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
        val candidate = ClassId(packageFqName, relativeClassName, isLocal = false)
        if (resolves(candidate)) return candidate
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

