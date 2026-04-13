/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
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
 */
class JavaResolutionContext private constructor(
    val packageFqName: FqName,
    private val simpleImports: Map<String, FqName>,
    private val starImports: List<FqName>,
    private val localClassProvider: (Name) -> JavaClass?,
    private val typeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    private val containingClassProvider: (() -> JavaClass?)? = null,
    private val classFinderProvider: (() -> JavaClassFinderOverAstImpl)? = null,
    /**
     * Type parameters from OUTER (enclosing) classes that have lower priority than inner class names.
     * Used for static nested types where outer type params are "in scope" per some Java compilers
     * (like PSI) but inner class names of the static nested type shadow them.
     * Distinct from [typeParametersInScope] which are the method/class OWN type params and take
     * priority over inner class names.
     */
    private val inheritedTypeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
) {
    /**
     * Finds a class by simple name. Checks:
     * 1. Inner classes of the containing class (if any)
     * 2. Sibling inner classes (inner classes of the outer class)
     * 3. Inner classes of supertypes (JLS 6.5.2 - inherited member types)
     * 4. Inner classes of outer classes' supertypes (for nested inner classes)
     * 5. Top-level classes in the same compilation unit
     */
    fun findLocalClass(name: Name): JavaClass? {
        val containingClass = containingClassProvider?.invoke()
        // First check inner classes of the containing class
        containingClass?.findInnerClass(name)?.let { return it }
        // Then check sibling inner classes (classes in the outer class)
        // This handles cases like: class J { class AImpl {} class A extends AImpl {} }
        containingClass?.outerClass?.findInnerClass(name)?.let { return it }
        // Then check inner classes of supertypes (inherited member types per JLS 6.5.2)
        // This handles cases like: class B extends A { ... } where A has inner class Y
        containingClass?.let { cls ->
            findInnerClassFromSupertypes(name, cls, mutableSetOf())?.let { return it }
        }
        // Also check inner classes of outer classes' supertypes
        // This handles nested inner class cases like: class Y extends X { class D { z ref; } }
        // where z is an inner class of X (Y's supertype)
        var outer = containingClass?.outerClass
        while (outer != null) {
            findInnerClassFromSupertypes(name, outer, mutableSetOf())?.let { return it }
            outer = outer.outerClass
        }
        // Then check top-level classes
        return localClassProvider(name)
    }
    
    /**
     * Searches for an inner class with the given name in the supertype hierarchy.
     * This implements JLS 6.5.2 - inherited member types are in scope.
     * 
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * which will cause MISSING_DEPENDENCY_CLASS error as per javac behavior.
     * 
     * Uses the classFinderProvider (if available) to detect cross-file ambiguities.
     * Falls back to local resolution for same-file supertypes.
     */
    private fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass, visited: MutableSet<JavaClass>): JavaClass? {
        if (javaClass in visited) return null
        visited.add(javaClass)
        
        val allFound = mutableSetOf<JavaClass>()

        // First try local resolution (same-file supertypes)
        for (supertype in javaClass.supertypes) {
            val supertypeRef = supertype.presentableText.let { text ->
                val withoutGenerics = text.substringBefore('<').trim()
                withoutGenerics.substringBefore('.').trim()
            }

            if (supertypeRef.isEmpty()) continue

            val supertypeClass = localClassProvider(Name.identifier(supertypeRef)) ?: continue

            supertypeClass.findInnerClass(name)?.let { found ->
                allFound.add(found)
            }

            findInnerClassFromSupertypes(name, supertypeClass, visited)?.let { found ->
                allFound.add(found)
            }
        }
        
        // If local resolution found nothing, try cross-file detection
        if (allFound.isEmpty()) {
            val javaClassOverAst = javaClass as? JavaClassOverAst
            if (javaClassOverAst != null && classFinderProvider != null) {
                val fqName = javaClassOverAst.fqName
                if (fqName != null) {
                    val containingClassId = fqNameToClassId(fqName)
                    val classFinder = classFinderProvider.invoke()

                    val inheritedInners = classFinder.collectInheritedInnerClasses(containingClassId)
                    val candidates = inheritedInners[name.asString()] ?: emptySet()

                    if (candidates.size > 1) {
                        // Ambiguity detected across multiple supertypes
                        return null
                    }

                    if (candidates.size == 1) {
                        return classFinder.findClass(JavaClassFinder.Request(candidates.first()))
                    }
                }
            }
        }

        if (allFound.size > 1) return null
        return allFound.firstOrNull()
    }

    /** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
    fun findTypeParameter(name: String): JavaTypeParameter? = typeParametersInScope[name]

    /** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    fun findInheritedTypeParameter(name: String): JavaTypeParameter? = inheritedTypeParametersInScope[name]

    fun getSimpleImport(simpleName: String): FqName? = simpleImports[simpleName]

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
        val newScope = typeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, localClassProvider, newScope, containingClassProvider, classFinderProvider, inheritedTypeParametersInScope
        )
    }

    /**
     * Creates a new context with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself.
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        val newInherited = inheritedTypeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, localClassProvider, typeParametersInScope, containingClassProvider, classFinderProvider, newInherited
        )
    }

    /**
     * Creates a new context for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(containingClass: JavaClass): JavaResolutionContext {
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, localClassProvider, typeParametersInScope,
            containingClassProvider = { containingClass },
            classFinderProvider = classFinderProvider,
            inheritedTypeParametersInScope = inheritedTypeParametersInScope
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
        // Handle nested class references like "Map.Entry"
        if (name.contains('.')) {
            return resolveNestedClassToClassId(name, tryResolve)
        }
        return resolveSimpleNameToClassId(name, tryResolve, getSupertypeClassIds)
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
    private fun resolveNestedClassToClassId(name: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        val parts = name.split('.')

        // Try resolving increasing prefixes as outer classes using normal resolution rules
        // This respects JLS 6.5.2: nested class takes priority when the outer class is in scope
        for (i in 1 until parts.size) {
            val outerParts = parts.subList(0, i)
            val nestedParts = parts.subList(i, parts.size)

            // Resolve the outer class using normal resolution (same package, imports, etc.)
            val outerClassId = if (outerParts.size > 1) {
                resolveNestedClassToClassId(outerParts.joinToString("."), tryResolve)
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
        // Use the cached collectInheritedInnerClasses map for BOTH ambiguity detection AND
        // as a fast path for inherited inner class resolution. This replaces the expensive BFS
        // in resolveInheritedInnerClassToClassId for the common case where the class finder is
        // available. The cache covers same-package source supertypes; for non-source supertypes
        // (Kotlin/binary classes), fall back to the BFS with getSupertypeClassIds callback.
        val containingClassForInherited = containingClassProvider?.invoke() as? JavaClassOverAst
        if (containingClassForInherited != null && classFinderProvider != null) {
            val classFinder = classFinderProvider.invoke()

            // Collect inherited inner classes for the containing class and all outer classes,
            // matching the scope that resolveInheritedInnerClassToClassId would walk.
            val allCandidates = mutableSetOf<ClassId>()
            var currentForInherited: JavaClass? = containingClassForInherited
            while (currentForInherited != null) {
                val jdClass = currentForInherited as? JavaClassOverAst
                val fqn = jdClass?.fqName
                if (fqn != null) {
                    val cid = fqNameToClassId(fqn)
                    val inheritedInners = classFinder.collectInheritedInnerClasses(cid)
                    val candidates = inheritedInners[simpleName] ?: emptySet()
                    allCandidates.addAll(candidates)
                }
                currentForInherited = currentForInherited.outerClass
            }

            when {
                allCandidates.size > 1 -> return null // Ambiguously inherited – don't resolve
                allCandidates.size == 1 -> {
                    val candidateClassId = allCandidates.first()
                    if (tryResolve(candidateClassId)) return candidateClassId
                }
                // allCandidates.isEmpty(): name is not an inherited source-level inner class.
                // Still need to check non-source supertypes (Kotlin/binary) via BFS Phase 2,
                // but only if getSupertypeClassIds callback is available.
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
        for (starPackage in starImports.distinct()) {
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
     * This handles cross-file inheritance (e.g., Java class extending Kotlin class with inner class).
     *
     * Walks the supertype hierarchy transitively using BFS. For each resolved supertype,
     * probes SupertypeClassId.SimpleName via [tryResolve]. For Java source supertypes,
     * also queues their own supertypes for deeper walking.
     *
     * Example: Derived extends Base, Base implements Map → Map.Entry is findable.
     */
    private fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? {
        val containingClass = containingClassProvider?.invoke() ?: return null

        // Collect direct supertypes from the containing class and its outer classes
        val initialSupertypes = mutableListOf<JavaClassifierType>()
        var currentClass: JavaClass? = containingClass
        while (currentClass != null) {
            initialSupertypes.addAll(currentClass.supertypes)
            currentClass = currentClass.outerClass
        }

        val visited = mutableSetOf<ClassId>()
        var foundClassId: ClassId? = null

        // Phase 1: BFS through JavaClassifierType objects (from Java model)
        var currentLevel: List<JavaClassifierType> = initialSupertypes
        // ClassIds of non-source supertypes that couldn't be walked via Java model
        val nonSourceSupertypeIds = mutableListOf<ClassId>()
        val maxDepth = 5

        for (depth in 0 until maxDepth) {
            if (currentLevel.isEmpty()) break
            val nextLevel = mutableListOf<JavaClassifierType>()

            for (supertype in currentLevel) {
                // Resolve the supertype using text-based resolution only (no resolve() calls
                // to avoid recursion back into resolveInheritedInnerClassToClassId).
                val supertypeName = supertype.presentableText.substringBefore('<').trim()
                if (supertypeName.isEmpty()) continue

                val supertypeClassId = if (supertypeName.contains('.')) {
                    resolveNestedClassToClassIdWithoutInheritance(supertypeName, tryResolve)
                } else {
                    resolveSimpleNameToClassIdWithoutInheritance(supertypeName, tryResolve)
                } ?: continue

                if (!visited.add(supertypeClassId)) continue

                // Try the inner class: SupertypeClassId.SimpleName
                val innerClassId = supertypeClassId.createNestedClassId(Name.identifier(simpleName))
                if (tryResolve(innerClassId)) {
                    if (foundClassId != null && foundClassId != innerClassId) {
                        return null // Ambiguity
                    }
                    foundClassId = innerClassId
                }

                // Queue deeper supertypes for the next BFS level.
                if (foundClassId == null) {
                    val classFinder = classFinderProvider?.invoke()
                    if (classFinder != null && classFinder.isClassInIndex(supertypeClassId)) {
                        // Java source class: walk via class finder (safe, no FIR interaction)
                        val javaClass = classFinder.findClass(JavaClassFinder.Request(supertypeClassId))
                        if (javaClass != null) {
                            nextLevel.addAll(javaClass.supertypes)
                        }
                    } else {
                        // Non-source class (Kotlin, binary): remember for Phase 2
                        nonSourceSupertypeIds.add(supertypeClassId)
                    }
                }
            }

            if (foundClassId != null) return foundClassId
            currentLevel = nextLevel
        }

        if (foundClassId != null) return foundClassId

        // Phase 2: For non-source supertypes (Kotlin/binary classes), use the FIR callback
        // to get their supertype ClassIds and probe for inner classes transitively.
        if (getSupertypeClassIds != null && nonSourceSupertypeIds.isNotEmpty()) {
            val queue = ArrayDeque(nonSourceSupertypeIds)
            var phase2Depth = 0
            while (queue.isNotEmpty() && phase2Depth < maxDepth) {
                val batch = queue.toList()
                queue.clear()
                for (classId in batch) {
                    for (parentClassId in getSupertypeClassIds(classId)) {
                        if (!visited.add(parentClassId)) continue

                        val innerClassId = parentClassId.createNestedClassId(Name.identifier(simpleName))
                        if (tryResolve(innerClassId)) {
                            if (foundClassId != null && foundClassId != innerClassId) return null
                            foundClassId = innerClassId
                        }
                        if (foundClassId == null) {
                            queue.add(parentClassId)
                        }
                    }
                }
                if (foundClassId != null) return foundClassId
                phase2Depth++
            }
        }

        return foundClassId
    }

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
        for (starPackage in starImports.distinct()) {
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
        // We know the package from resolutionContext.packageFqName
        // The class name is whatever comes after the package
        val fqnString = fqName.asString()
        val pkgString = packageFqName.asString()
        
        val className = if (pkgString.isEmpty()) {
            fqnString
        } else if (fqnString.startsWith(pkgString + ".")) {
            fqnString.substring(pkgString.length + 1)
        } else {
            fqnString
        }
        
        return ClassId(packageFqName, FqName(className), isLocal = false)
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
            classFinderProvider: (() -> JavaClassFinderOverAstImpl)? = null
        ): JavaResolutionContext {
            val packageFqName = extractPackageName(root)
            val (simpleImports, starImports) = extractImports(root)

            // Local classes indexed lazily to avoid circular initialization
            var contextRef: JavaResolutionContext? = null
            val localClassCache = mutableMapOf<Name, JavaClass>()

            val localClassProvider: (Name) -> JavaClass? = { name ->
                localClassCache[name] ?: findClassNode(root, name)?.let { classNode ->
                    JavaClassOverAst(classNode, contextRef!!, outerClass = null).also {
                        localClassCache[name] = it
                    }
                }
            }

            return JavaResolutionContext(
                packageFqName = packageFqName,
                simpleImports = simpleImports,
                starImports = starImports,
                localClassProvider = localClassProvider,
                classFinderProvider = classFinderProvider
            ).also { contextRef = it }
        }

        private fun extractPackageName(root: JavaSyntaxNode): FqName {
            val packageStmt = root.findChildByType("PACKAGE_STATEMENT")
            val packageName = packageStmt?.findChildByType("JAVA_CODE_REFERENCE")?.text
            return if (packageName != null) FqName(packageName) else FqName.ROOT
        }

        private fun extractImports(root: JavaSyntaxNode): Pair<Map<String, FqName>, List<FqName>> {
            val simpleImports = mutableMapOf<String, FqName>()
            val starImports = mutableListOf<FqName>()

            // Handle case where root might be CLASS instead of compilation unit
            val importList = root.findChildByType("IMPORT_LIST")
                ?: root.findChildByType("CLASS")?.findChildByType("IMPORT_LIST")

            for (importNode in importList?.getChildrenByType("IMPORT_STATEMENT") ?: emptyList()) {
                val codeRef = importNode.findChildByType("JAVA_CODE_REFERENCE") ?: continue
                val hasStar = importNode.children.any { it.type.toString() == "ASTERISK" }
                val fqName = codeRef.text

                if (hasStar) {
                    starImports.add(FqName(fqName))
                } else {
                    // Keep first occurrence: duplicate explicit imports for the same simple name
                    // are a compile error in Java. PSI uses first-seen semantics, so we do too.
                    val simpleName = fqName.substringAfterLast('.')
                    if (!simpleImports.containsKey(simpleName)) {
                        simpleImports[simpleName] = FqName(fqName)
                    }
                }
            }

            // Handle static imports: "import static pkg.Class.MEMBER" or "import static pkg.Class.*"
            // The KMP parser uses IMPORT_STATIC_STATEMENT with IMPORT_STATIC_REFERENCE child.
            for (importNode in importList?.getChildrenByType("IMPORT_STATIC_STATEMENT") ?: emptyList()) {
                val refNode = importNode.findChildByType("IMPORT_STATIC_REFERENCE") ?: continue
                val hasStar = importNode.children.any { it.type.toString() == "ASTERISK" }
                val fqName = refNode.text

                if (hasStar) {
                    starImports.add(FqName(fqName))
                } else {
                    // e.g. "example.KotlinDtoMapping.ID" → simpleName = "ID"
                    val simpleName = fqName.substringAfterLast('.')
                    if (!simpleImports.containsKey(simpleName)) {
                        simpleImports[simpleName] = FqName(fqName)
                    }
                }
            }

            // Also check for ERROR_ELEMENT imports (parser may emit these for imports starting with reserved words like 'kotlin')
            for (errorNode in importList?.getChildrenByType("ERROR_ELEMENT") ?: emptyList()) {
                if (errorNode.findChildByType("IMPORT_KEYWORD") == null) continue
                
                // Reconstruct the import from IDENTIFIER and DOT children
                val identifiers = mutableListOf<String>()
                for (child in errorNode.children) {
                    if (child.type.toString() == "IDENTIFIER") {
                        identifiers.add(child.text)
                    }
                }
                if (identifiers.isEmpty()) continue
                
                val hasStar = errorNode.children.any { it.type.toString() == "ASTERISK" }
                val fqName = identifiers.joinToString(".")
                
                if (hasStar) {
                    starImports.add(FqName(fqName))
                } else {
                    val simpleName = identifiers.last()
                    if (!simpleImports.containsKey(simpleName)) {
                        simpleImports[simpleName] = FqName(fqName)
                    }
                }
            }
            
            // Handle fragmented import patterns where parser splits import across sibling nodes
            // Pattern 1: ERROR_ELEMENT(IMPORT_KEYWORD) followed by TYPE(JAVA_CODE_REFERENCE) - simple import
            // Pattern 2: ERROR_ELEMENT(import) followed by TYPE(pkg.) followed by ERROR_ELEMENT(*;) - star import
            // Parser may insert MODIFIER_LIST and other nodes between them
            val allChildren = root.children
            var i = 0
            while (i < allChildren.size) {
                val node = allChildren[i]
                val nodeType = node.type.toString()
                
                // Check for ERROR_ELEMENT containing "import" keyword or text
                val isImportError = nodeType == "ERROR_ELEMENT" && 
                    (node.findChildByType("IMPORT_KEYWORD") != null || node.text.trim() == "import")
                
                if (isImportError) {
                    // Look for the next TYPE or JAVA_CODE_REFERENCE sibling, skipping whitespace and modifier list
                    var typeNode: JavaSyntaxNode? = null
                    var hasStar = false
                    
                    for (j in (i + 1) until allChildren.size) {
                        val sibling = allChildren[j]
                        val sibType = sibling.type.toString()
                        // Skip whitespace, empty modifier lists, and empty error elements
                        if (sibType == "WHITE_SPACE" || sibType == "MODIFIER_LIST") continue
                        if (sibType == "ERROR_ELEMENT" && sibling.text.isBlank()) continue
                        
                        if (sibType == "TYPE" || sibType == "JAVA_CODE_REFERENCE") {
                            typeNode = sibling
                            // Continue to check for star in following siblings (not just the next one)
                            for (k in (j + 1) until allChildren.size) {
                                val nextSib = allChildren[k]
                                val nextSibType = nextSib.type.toString()
                                if (nextSibType == "WHITE_SPACE" || nextSibType == "MODIFIER_LIST") continue
                                if (nextSibType == "ERROR_ELEMENT" && nextSib.text.isBlank()) continue
                                if (nextSibType == "ERROR_ELEMENT" && nextSib.text.contains("*")) {
                                    hasStar = true
                                    break
                                }
                                // Stop at CLASS or other significant nodes (interfaces/enums are also CLASS nodes)
                                if (nextSibType == "CLASS") break
                            }
                            break
                        }
                        // Also check if ERROR_ELEMENT itself contains star (like "*;")
                        if (sibType == "ERROR_ELEMENT" && sibling.text.contains("*")) {
                            hasStar = true
                        }
                        // Stop at CLASS or other significant nodes (interfaces/enums are also CLASS nodes)
                        if (sibType == "CLASS") break
                    }
                    
                    if (typeNode != null) {
                        val ref = typeNode.findChildByType("JAVA_CODE_REFERENCE") ?: typeNode
                        var fqName = ref.text.trim()
                        // Remove trailing dot if present (from fragmented star import like "org.jetbrains.annotations.")
                        if (fqName.endsWith('.')) {
                            fqName = fqName.dropLast(1)
                        }
                        
                        if (fqName.contains('.')) {
                            if (hasStar) {
                                starImports.add(FqName(fqName))
                            } else {
                                val simpleName = fqName.substringAfterLast('.')
                                if (!simpleImports.containsKey(simpleName)) {
                                    simpleImports[simpleName] = FqName(fqName)
                                }
                            }
                        }
                    }
                }
                i++
            }

            return simpleImports to starImports
        }

        private fun findClassNode(root: JavaSyntaxNode, name: Name): JavaSyntaxNode? {
            for (child in root.children) {
                if (child.type.toString() == "CLASS") {
                    val id = child.findChildByType("IDENTIFIER")?.text
                    if (id == name.asString()) return child
                }
            }
            return null
        }
    }
}
