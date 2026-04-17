/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolution context for Java source files. Encapsulates all information
 * needed to resolve type references within a compilation unit.
 *
 * This is analogous to FIR scopes but simplified for Java's scoping rules.
 * The typeParametersInScope tracks type parameters visible at the current location
 * (from containing class and method declarations).
 *
 * Delegates to focused implementations:
 * - [JavaImportResolver] — import extraction and package name parsing (stateless)
 * - [JavaScopeResolver] — type parameter scoping and local class lookup
 * - [JavaInheritedMemberResolver] — supertype hierarchy traversal for inner classes
 */
class JavaResolutionContext private constructor(
    val packageFqName: FqName,
    private val simpleImports: Map<String, FqName>,
    private val starImports: List<FqName>,
    private val distinctStarImports: List<FqName> = starImports.distinct(),
    private val scopeResolver: JavaScopeResolver,
    private val inheritedMemberResolver: JavaInheritedMemberResolver,
    private val containingClassProvider: (() -> JavaClass?)? = null,
    private val classFinderProvider: (() -> JavaClassFinderOverAstImpl)? = null,
    /**
     * Lazily computed aggregated inherited inner classes for the entire containing class chain.
     * Maps simpleName -> Set<ClassId> across the containing class and all its outer classes.
     * Cached to avoid re-walking the outer class chain on every [resolveSimpleNameToClassId] call.
     * Shared across contexts with the same containing class (via [withTypeParameters] / [withInheritedTypeParameters]).
     * Array of size 1 used as a mutable holder so it can be shared by reference.
     */
    @Suppress("ArrayInDataClass")
    private val aggregatedInheritedInnerClassesHolder: Array<Map<String, Set<ClassId>>?> = arrayOfNulls(1),
) {
    private fun getAggregatedInheritedInnerClasses(): Map<String, Set<ClassId>>? {
        aggregatedInheritedInnerClassesHolder[0]?.let { return it }
        val containingClass = containingClassProvider?.invoke() as? JavaClassOverAst ?: return null
        val result = inheritedMemberResolver.computeAggregatedInheritedInnerClasses(containingClass)
        aggregatedInheritedInnerClassesHolder[0] = result
        return result
    }

    /**
     * Finds a class by simple name. Checks:
     * 1. Inner classes of the containing class (if any)
     * 2. Sibling inner classes (inner classes of the outer class)
     * 3. Inner classes of supertypes (JLS 6.5.2 - inherited member types)
     * 4. Inner classes of outer classes' supertypes (for nested inner classes)
     * 5. Top-level classes in the same compilation unit
     */
    fun findLocalClass(name: Name): JavaClass? = scopeResolver.findLocalClass(name)

    /**
     * Searches the supertype hierarchy of [outerClassId] for an inherited nested class with [nestedName].
     * Uses both the [getSupertypeClassIds] callback (for Kotlin/binary classes) and the class finder's
     * [JavaClassFinderOverAstImpl.collectInheritedInnerClasses] (for same-package Java source classes).
     */
    private fun findInheritedNestedClass(
        outerClassId: ClassId,
        nestedName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: (ClassId) -> List<ClassId>,
        visited: MutableSet<ClassId>,
    ): ClassId? = inheritedMemberResolver.findInheritedNestedClass(outerClassId, nestedName, tryResolve, getSupertypeClassIds, visited)

    /** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
    fun findTypeParameter(name: String): JavaTypeParameter? = scopeResolver.findTypeParameter(name)

    /** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    fun findInheritedTypeParameter(name: String): JavaTypeParameter? = scopeResolver.findInheritedTypeParameter(name)

    fun getSimpleImport(simpleName: String): FqName? = simpleImports[simpleName]

    /**
     * Returns the parsed imports (simple + star) from this context.
     * Used by [JavaClassFinderOverAstImpl.getDirectSupertypes] on the fast path
     * to avoid re-extracting imports from the AST root.
     */
    internal fun getImports(): Pair<Map<String, FqName>, List<FqName>> = Pair(simpleImports, starImports)

    /**
     * Returns true if the import target for [simpleName] is resolvable as a Java class.
     *
     * Checks whether the import target exists in the Java source index or is available
     * as a binary (compiled) Java class on the classpath. This matches PSI behavior where
     * only classes resolvable through PSI/classpath indexes are eagerly resolved.
     *
     * Kotlin classes (builtins, source classes without light classes) are NOT resolvable
     * through PSI indexes, so this returns false for them. FIR handles such classes
     * through its own symbol provider instead.
     *
     * Returns true (conservative) when no class finder is available.
     */
    fun isImportTargetAvailableAsJavaClass(simpleName: String): Boolean {
        val importedFqn = simpleImports[simpleName] ?: return false
        val fqnStr = importedFqn.asString()
        // Imports from kotlin.* packages point to Kotlin classes, not Java classes.
        // PSI can't resolve Kotlin classes through its Java indexes (no light classes
        // in K2 mode), so they appear as unresolved types. Match this behavior by
        // not eagerly resolving kotlin.* imports.
        if (fqnStr.startsWith("kotlin.") || fqnStr == "kotlin") return false
        // All other imports (JDK, library, user-defined Java classes) are assumed
        // to be resolvable as Java classes
        return true
    }

    /**
     * Returns the first star import package that could contain a class with the given simple name.
     * Used for best-effort classId resolution when we can't call the symbol provider.
     */
    fun getFirstStarImportCandidate(simpleName: String): ClassId? {
        val starPackage = starImports.firstOrNull() ?: return null
        return ClassId(starPackage, Name.identifier(simpleName))
    }

    /**
     * Returns true if a class with [simpleName] can be UNAMBIGUOUSLY found in the source index.
     * Checks: explicit imports, same-package, star imports (with ambiguity detection).
     *
     * Uses index-only lookup (no file I/O, no class instantiation) so it is safe to call
     * during FIR type processing without causing initialization order issues.
     *
     * Used by [JavaClassifierTypeOverAst.isTriviallyFlexibleHint] to make FIR produce compact
     * `T!` rendering (isTrivial=true) instead of `ft<T, T?>` for user-defined Java source classes,
     * matching the PSI behavior where all resolved Java classes are trivially flexible.
     *
     * Returns false for ambiguous cases (multiple star-import matches) to avoid false positives.
     */
    fun isUnambiguouslyCrossFileClass(simpleName: String): Boolean {
        val finder = classFinderProvider?.invoke() ?: return false
        // 1. Explicit single-type import takes highest priority (JLS 7.5.1) — always unambiguous
        simpleImports[simpleName]?.let { importedFqn ->
            val fqnStr = importedFqn.asString()
            val classId = if (fqnStr.contains('.')) {
                val lastDot = fqnStr.lastIndexOf('.')
                ClassId(FqName(fqnStr.substring(0, lastDot)), FqName(fqnStr.substring(lastDot + 1)), isLocal = false)
            } else {
                ClassId.topLevel(FqName(fqnStr))
            }
            if (finder.isClassInIndex(classId)) return true
        }
        // 2. Same-package class — always unambiguous
        val samePackageClassId = if (packageFqName.isRoot) {
            ClassId.topLevel(FqName(simpleName))
        } else {
            ClassId(packageFqName, Name.identifier(simpleName))
        }
        if (finder.isClassInIndex(samePackageClassId)) return true
        // 3. Star imports — only if exactly one star import provides the class (no ambiguity)
        val starMatches = starImports.count { starPackage ->
            finder.isClassInIndex(ClassId(starPackage, Name.identifier(simpleName)))
        }
        return starMatches == 1
    }

    /**
     * Creates a new context with additional OWN type parameters (high priority).
     * Used when entering a class or method that declares type parameters.
     * Own type params take priority over inner class names of the containing class.
     */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, distinctStarImports,
            scopeResolver.withTypeParameters(typeParams),
            inheritedMemberResolver,
            containingClassProvider, classFinderProvider,
            aggregatedInheritedInnerClassesHolder, // share — containingClass unchanged
        )
    }

    /**
     * Creates a new context with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself.
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, distinctStarImports,
            scopeResolver.withInheritedTypeParameters(typeParams),
            inheritedMemberResolver,
            containingClassProvider, classFinderProvider,
            aggregatedInheritedInnerClassesHolder, // share — containingClass unchanged
        )
    }

    /**
     * Creates a new context for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(containingClass: JavaClass): JavaResolutionContext {
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, distinctStarImports,
            scopeResolver.withContainingClass(containingClass),
            inheritedMemberResolver,
            containingClassProvider = { containingClass },
            classFinderProvider = classFinderProvider,
            // new holder — containingClass changed, aggregated inherited inner classes may differ
        )
    }

    /**
     * Resolve a type name to a ClassId using the callback for external resolution.
     * 
     * This method returns a ClassId directly, which unambiguously encodes the package/class
     * boundary. For example, "a.b" could mean either:
     * - ClassId("a", "b") - package "a", class "b"
     * - ClassId("", "a.b") - root package, nested class "a.b"
     * 
     * Using ClassId avoids the ambiguity that string-based resolution has.
     */
    fun resolve(
        name: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? {
        // Cache tryResolve results within this invocation. The recursive prefix splitting
        // in resolveNestedClassToClassId probes the same ClassIds many times (e.g., "com"
        // is tried as a class for each prefix of "com.google.protobuf.Foo"). The callback
        // is deterministic within a single resolve() call, so caching is safe.
        val cache = HashMap<ClassId, Boolean>()
        val cachedTryResolve: (ClassId) -> Boolean = { classId ->
            cache.getOrPut(classId) { tryResolve(classId) }
        }
        // Handle nested class references like "Map.Entry"
        if (name.contains('.')) {
            return resolveNestedClassToClassId(name, cachedTryResolve, getSupertypeClassIds)
        }
        return resolveSimpleNameToClassId(name, cachedTryResolve, getSupertypeClassIds)
    }

    /**
     * Resolve a nested class reference to ClassId.
     * 
     * Per JLS 6.5.2, when a qualified name Q.Id could refer to either:
     * - A nested class Id of class Q, or
     * - A top-level class Id in package Q
     * 
     * The nested class interpretation takes priority, BUT only if Q actually resolves
     * to a class in the current scope. We try to resolve Q as a class first using
     * the normal resolution rules (same package, imports, etc.). If Q resolves to a class,
     * we try Q.Id as a nested class. If that fails or Q doesn't resolve, we fall back
     * to trying Q.Id as a fully qualified name.
     */
    private fun resolveNestedClassToClassId(
        name: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? {
        val parts = name.split('.')

        // Try resolving increasing prefixes as outer classes using normal resolution rules
        // This respects JLS 6.5.2: nested class takes priority when the outer class is in scope
        for (i in 1 until parts.size) {
            val outerParts = parts.subList(0, i)
            val nestedParts = parts.subList(i, parts.size)

            // Resolve the outer class using normal resolution (same package, imports, etc.)
            val outerClassId = if (outerParts.size > 1) {
                resolveNestedClassToClassId(outerParts.joinToString("."), tryResolve, getSupertypeClassIds)
            } else {
                // For single-part outer name, use resolveSimpleNameToClassId which
                // checks same-package, java.lang, imports - the normal resolution order
                resolveSimpleNameToClassId(outerParts[0], tryResolve)
            }

            if (outerClassId != null) {
                // Build the nested class ClassId by appending to the relative class name
                val nestedClassName = FqName.fromSegments(
                    outerClassId.relativeClassName.pathSegments().map { it.asString() } + nestedParts
                )
                val nestedClassId = ClassId(outerClassId.packageFqName, nestedClassName, isLocal = false)
                if (tryResolve(nestedClassId)) return nestedClassId

                // Nested class not directly declared — search supertypes for inherited inner classes.
                // This handles cases like SimpleFunctionDescriptor.CopyBuilder where CopyBuilder is
                // declared in FunctionDescriptor (superinterface) but referenced via SimpleFunctionDescriptor.
                if (nestedParts.size == 1 && getSupertypeClassIds != null) {
                    val inherited = findInheritedNestedClass(
                        outerClassId, nestedParts[0], tryResolve, getSupertypeClassIds, mutableSetOf()
                    )
                    if (inherited != null) return inherited
                }
            }
        }

        // Also try inherited inner class resolution via the aggregated map from the class finder.
        // This covers same-package source supertypes that the getSupertypeClassIds callback
        // might not see (Java class supertypes are excluded from the callback to avoid premature resolution).
        if (getSupertypeClassIds == null && classFinderProvider != null && parts.size == 2) {
            val outerClassId = resolveSimpleNameToClassId(parts[0], tryResolve)
            if (outerClassId != null) {
                val classFinder = classFinderProvider.invoke()
                val inheritedInners = classFinder.collectInheritedInnerClasses(outerClassId)
                val candidates = inheritedInners[parts[1]]
                if (candidates != null && candidates.size == 1) {
                    val candidateClassId = candidates.first()
                    if (tryResolve(candidateClassId)) return candidateClassId
                }
            }
        }

        // Fall back: try as fully qualified name with different package/class splits
        // Try from longest package to shortest (standard resolution order)
        for (classStartIndex in (parts.size - 1) downTo 0) {
            val packageFqName = if (classStartIndex == 0) {
                FqName.ROOT
            } else {
                FqName.fromSegments(parts.subList(0, classStartIndex))
            }
            val relativeClassName = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
            val classId = ClassId(packageFqName, relativeClassName, isLocal = false)
            if (tryResolve(classId)) return classId
        }

        return null
    }

    /**
     * Resolve a simple (non-nested) type name to ClassId.
     */
    private fun resolveSimpleNameToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? {
        // 1. Explicit single-type imports take highest priority (JLS 7.5.1)
        // Use resolveAsClassId to handle nested class FQNs like "a.x.b.b.b" where
        // ClassId.topLevel would incorrectly split as package="a.x.b.b", class="b".
        simpleImports[simpleName]?.let { imported ->
            resolveAsClassId(imported, tryResolve)?.let { return it }
        }

        // 2. Local/inner classes (same compilation unit, containing class hierarchy, supertypes)
        // This handles cases like inner classes and inherited member types (JLS 6.5.2)
        findLocalClass(Name.identifier(simpleName))?.let { localClass ->
            val fqName = localClass.fqName
            if (fqName != null) {
                val classId = fqNameToClassId(fqName)
                if (tryResolve(classId)) return classId
            }
        }

        // 2b. Inherited inner classes from supertypes (cross-file, e.g., Kotlin classes)
        // JLS 6.5.2: inherited member types are in scope
        //
        // Use the aggregated inherited inner classes map (cached per context) for BOTH
        // ambiguity detection AND as a fast path. The map covers the containing class chain
        // and is computed once per context, avoiding repeated outer-class-chain walks.
        val aggregatedInherited = getAggregatedInheritedInnerClasses()
        if (aggregatedInherited != null) {
            val allCandidates = aggregatedInherited[simpleName] ?: emptySet()

            when {
                allCandidates.size > 1 -> return null // Ambiguously inherited – don't resolve
                allCandidates.size == 1 -> {
                    val candidateClassId = allCandidates.first()
                    if (tryResolve(candidateClassId)) return candidateClassId
                }
                // allCandidates.isEmpty(): name is not an inherited source-level inner class.
                // Only fall back to BFS when getSupertypeClassIds callback is available,
                // since the BFS Phase 2 needs it for non-source (Kotlin/binary) supertypes.
                // Without the callback, the aggregated map already covers all source supertypes.
                else -> {
                    if (getSupertypeClassIds != null) {
                        val inheritedResult = resolveInheritedInnerClassToClassId(simpleName, tryResolve, getSupertypeClassIds)
                        if (inheritedResult != null) return inheritedResult
                    }
                }
            }
        } else {
            // No class finder available — use the full BFS as fallback
            val inheritedResult = resolveInheritedInnerClassToClassId(simpleName, tryResolve, getSupertypeClassIds)
            if (inheritedResult != null) return inheritedResult
        }

        // 3. Same package
        val samePackageClassId = ClassId(packageFqName, Name.identifier(simpleName))
        if (tryResolve(samePackageClassId)) return samePackageClassId

        // 4. java.lang.*
        val javaLangClassId = ClassId(FqName("java.lang"), Name.identifier(simpleName))
        if (JavaToKotlinClassMap.mapJavaToKotlin(javaLangClassId.asSingleFqName()) != null || tryResolve(javaLangClassId)) {
            return javaLangClassId
        }

        // 5. Explicit star imports
        // Handle both package-level (import java.util.*) and class-level (import a.D.*) star imports.
        // Class-level: "import a.D.*" imports nested types of class a.D.
        var foundClassId: ClassId? = null
        for (starPackage in distinctStarImports) {
            val candidateClassId = ClassId(starPackage, Name.identifier(simpleName))
            if (tryResolve(candidateClassId)) {
                if (foundClassId != null && foundClassId != candidateClassId) return null // Ambiguous
                foundClassId = candidateClassId
            } else {
                // Try class-level star import: "import a.D.*" → resolve a.D as a class,
                // then look for nested class `simpleName` within it.
                val outerClassId = resolveAsClassId(starPackage, tryResolve)
                if (outerClassId != null) {
                    val nestedClassId = outerClassId.createNestedClassId(Name.identifier(simpleName))
                    if (tryResolve(nestedClassId)) {
                        if (foundClassId != null && foundClassId != nestedClassId) return null // Ambiguous
                        foundClassId = nestedClassId
                    }
                }
            }
        }
        if (foundClassId != null) return foundClassId

        return null
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes.
     * Delegates to [JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId].
     */
    private fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? = inheritedMemberResolver.resolveInheritedInnerClassToClassId(
        simpleName, tryResolve, getSupertypeClassIds, containingClassProvider,
        resolveWithoutInheritance = { name, resolve ->
            if (name.contains('.')) resolveNestedClassToClassIdWithoutInheritance(name, resolve)
            else resolveSimpleNameToClassIdWithoutInheritance(name, resolve)
        }
    )

    /**
     * Resolve a nested class reference without checking inherited inner classes (to avoid infinite recursion).
     */
    private fun resolveNestedClassToClassIdWithoutInheritance(name: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        val parts = name.split('.')

        // Try resolving increasing prefixes as outer classes
        for (i in 1 until parts.size) {
            val outerParts = parts.subList(0, i)
            val nestedParts = parts.subList(i, parts.size)

            val outerClassId = if (outerParts.size > 1) {
                resolveNestedClassToClassIdWithoutInheritance(outerParts.joinToString("."), tryResolve)
            } else {
                resolveSimpleNameToClassIdWithoutInheritance(outerParts[0], tryResolve)
            }

            if (outerClassId != null) {
                val nestedClassName = FqName.fromSegments(
                    outerClassId.relativeClassName.pathSegments().map { it.asString() } + nestedParts
                )
                val nestedClassId = ClassId(outerClassId.packageFqName, nestedClassName, isLocal = false)
                if (tryResolve(nestedClassId)) return nestedClassId
            }
        }

        // Fall back: try as fully qualified name with different package/class splits
        for (classStartIndex in (parts.size - 1) downTo 0) {
            val packageFqName = if (classStartIndex == 0) {
                FqName.ROOT
            } else {
                FqName.fromSegments(parts.subList(0, classStartIndex))
            }
            val relativeClassName = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
            val classId = ClassId(packageFqName, relativeClassName, isLocal = false)
            if (tryResolve(classId)) return classId
        }

        return null
    }

    /**
     * Resolve a simple name without checking inherited inner classes (to avoid infinite recursion).
     */
    private fun resolveSimpleNameToClassIdWithoutInheritance(simpleName: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        // Explicit imports
        simpleImports[simpleName]?.let { imported ->
            val classId = ClassId.topLevel(imported)
            if (tryResolve(classId)) return classId
        }

        // Same package
        val samePackageClassId = ClassId(packageFqName, Name.identifier(simpleName))
        if (tryResolve(samePackageClassId)) return samePackageClassId

        // java.lang.*
        val javaLangClassId = ClassId(FqName("java.lang"), Name.identifier(simpleName))
        if (JavaToKotlinClassMap.mapJavaToKotlin(javaLangClassId.asSingleFqName()) != null || tryResolve(javaLangClassId)) {
            return javaLangClassId
        }

        // Star imports
        for (starPackage in distinctStarImports) {
            val candidateClassId = ClassId(starPackage, Name.identifier(simpleName))
            if (tryResolve(candidateClassId)) return candidateClassId
        }

        return null
    }

    /**
     * Resolves a FqName to a ClassId by trying all possible package/class splits,
     * using the tryResolve callback to validate each candidate.
     *
     * Unlike ClassId.topLevel which only tries the trivial split at the last dot,
     * this tries all splits from longest package to shortest, so "a.x.b.b.b" will
     * try ClassId(a.x.b.b, b), ClassId(a.x.b, b.b), ClassId(a.x, b.b.b), ClassId(a, x.b.b.b).
     *
     * Used for explicit imports with nested class FQNs and for class-level star import resolution.
     */
    private fun resolveAsClassId(fqName: FqName, tryResolve: (ClassId) -> Boolean): ClassId? {
        val parts = fqName.pathSegments()
        if (parts.isEmpty()) return null
        for (classStartIndex in (parts.size - 1) downTo 0) {
            val pkg = if (classStartIndex == 0) FqName.ROOT
            else FqName.fromSegments(parts.subList(0, classStartIndex).map { it.asString() })
            val cls = FqName.fromSegments(parts.subList(classStartIndex, parts.size).map { it.asString() })
            val classId = ClassId(pkg, cls, false)
            if (tryResolve(classId)) return classId
        }
        return null
    }

    private fun fqNameToClassId(fqName: FqName): ClassId {
        return JavaInheritedMemberResolver.fqNameToClassId(fqName, packageFqName)
    }

    /**
     * Returns the ClassIds of the containing class chain, from innermost to outermost.
     * Used by java-direct types to expose the containing class hierarchy to FIR.
     */
    fun getContainingClassIds(): List<ClassId> {
        val result = mutableListOf<ClassId>()
        var cls: JavaClass? = containingClassProvider?.invoke()
        while (cls != null) {
            val fqName = cls.fqName
            if (fqName != null) {
                result.add(fqNameToClassId(fqName))
            }
            cls = cls.outerClass
        }
        return result
    }

    companion object {
        fun create(
            root: JavaSyntaxNode,
            classFinderProvider: (() -> JavaClassFinderOverAstImpl)? = null,
        ): JavaResolutionContext {
            val packageFqName = JavaImportResolver.extractPackageName(root)
            val (simpleImports, starImports) = JavaImportResolver.extractImports(root)

            // Local classes indexed lazily to avoid circular initialization
            var contextRef: JavaResolutionContext? = null
            val localClassCache = mutableMapOf<Name, JavaClass>()

            val localClassProvider: (Name) -> JavaClass? = { name ->
                localClassCache[name] ?: JavaImportResolver.findClassNode(root, name)?.let { classNode ->
                    JavaClassOverAst(classNode, contextRef!!, outerClass = null).also {
                        localClassCache[name] = it
                    }
                }
            }

            val inheritedMemberResolver = JavaInheritedMemberResolver(
                packageFqName, classFinderProvider, localClassProvider,
            )
            val scopeResolver = JavaScopeResolver(
                localClassProvider,
                containingClassProvider = null,
                inheritedMemberResolver,
            )

            return JavaResolutionContext(
                packageFqName = packageFqName,
                simpleImports = simpleImports,
                starImports = starImports,
                scopeResolver = scopeResolver,
                inheritedMemberResolver = inheritedMemberResolver,
                classFinderProvider = classFinderProvider,
            ).also { contextRef = it }
        }

        /**
         * Extracts imports from a root AST node.
         * Delegates to [JavaImportResolver.extractImports].
         */
        internal fun extractImports(root: JavaSyntaxNode): Pair<Map<String, FqName>, List<FqName>> =
            JavaImportResolver.extractImports(root)
    }
}
