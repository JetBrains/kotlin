/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
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
     * IMPORTANT: To avoid infinite recursion, we resolve supertypes using localClassProvider
     * directly instead of going through classifier (which calls findLocalClass).
     * This means we only search local (same compilation unit) supertypes.
     * For external supertypes, the resolution callback (resolveWithCallback) handles it via FIR.
     */
    private fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass, visited: MutableSet<JavaClass>): JavaClass? {
        // Cycle detection using object identity
        if (javaClass in visited) return null
        visited.add(javaClass)
        
        val allFound = mutableSetOf<JavaClass>()
        
        for (supertype in javaClass.supertypes) {
            // Get the supertype's raw name (first part before any dots or generics)
            // presentableText gives us the raw reference text like "x" or "List<String>"
            val supertypeRef = supertype.presentableText.let { text ->
                // Extract simple name (before '<' for generics, first part for qualified)
                val withoutGenerics = text.substringBefore('<').trim()
                withoutGenerics.substringBefore('.').trim()
            }
            
            if (supertypeRef.isEmpty()) continue
            
            // Try to find this supertype as a local class (using localClassProvider to avoid recursion)
            val supertypeClass = localClassProvider(Name.identifier(supertypeRef)) ?: continue
            
            // Check direct inner class of the supertype
            supertypeClass.findInnerClass(name)?.let { found ->
                allFound.add(found)
            }
            
            // Recursively check supertypes of the supertype
            findInnerClassFromSupertypes(name, supertypeClass, visited)?.let { found ->
                allFound.add(found)
            }
        }
        
        // Return null if ambiguous (multiple different classes found)
        if (allFound.size > 1) {
            return null
        }
        return allFound.firstOrNull()
    }

    fun findTypeParameter(name: String): JavaTypeParameter? = typeParametersInScope[name]

    fun getSimpleImport(simpleName: String): FqName? = simpleImports[simpleName]

    /**
     * Creates a new context with additional type parameters in scope.
     * Used when entering a class or method that declares type parameters.
     */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        val newScope = typeParametersInScope + typeParams.associateBy { it.name.asString() }
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, localClassProvider, newScope, containingClassProvider
        )
    }

    /**
     * Creates a new context for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(containingClass: JavaClass): JavaResolutionContext {
        return JavaResolutionContext(
            packageFqName, simpleImports, starImports, localClassProvider, typeParametersInScope
        ) { containingClass }
    }

    /**
     * Resolve a type name using the callback for external resolution.
     * Called by [JavaClassifierTypeOverAst.resolve] when FIR needs to resolve star imports.
     *
     * Resolution order (per JLS):
     * 1. Same package
     * 2. java.lang.* (automatic import)
     * 3. Explicit star imports
     *
     * For nested class references (e.g., "Map.Entry"), the outer class is resolved first,
     * then the nested class name is appended.
     */
    fun resolveWithCallback(name: String, tryResolve: (String) -> Boolean): String? {
        // Handle nested class references like "Map.Entry"
        if (name.contains('.')) {
            return resolveNestedClass(name, tryResolve)
        }
        return resolveSimpleName(name, tryResolve)
    }

    /**
     * Resolve a nested class reference like "Map.Entry" or "Outer.Inner.Deep".
     */
    private fun resolveNestedClass(name: String, tryResolve: (String) -> Boolean): String? {
        val parts = name.split('.')

        // Try resolving increasing prefixes as the outer class
        // For "A.B.C", try: A (then A.B.C), A.B (then A.B.C)
        for (i in 1 until parts.size) {
            val outerParts = parts.subList(0, i).joinToString(".")
            val nestedParts = parts.subList(i, parts.size).joinToString(".")

            // Resolve the outer class
            val resolvedOuter = if (outerParts.contains('.')) {
                resolveNestedClass(outerParts, tryResolve)
            } else {
                resolveSimpleName(outerParts, tryResolve)
            }
            
            if (resolvedOuter != null) {
                // Try the full nested class path
                val fullNested = "$resolvedOuter.$nestedParts"
                if (tryResolve(fullNested)) return fullNested
            }
        }
        
        // Also try as a fully qualified name directly
        if (tryResolve(name)) return name
        
        return null
    }

    /**
     * Resolve a simple (non-nested) type name.
     */
    private fun resolveSimpleName(simpleName: String, tryResolve: (String) -> Boolean): String? {
        // 1. Same package
        if (!packageFqName.isRoot) {
            val samePackageFqn = "${packageFqName.asString()}.$simpleName"
            if (tryResolve(samePackageFqn)) return samePackageFqn
        }

        // 2. java.lang.*
        val javaLangFqn = "java.lang.$simpleName"
        if (JavaToKotlinClassMap.mapJavaToKotlin(FqName(javaLangFqn)) != null || tryResolve(javaLangFqn)) {
            return javaLangFqn
        }

        // 3. Explicit star imports
        var foundFqn: String? = null
        for (starPackage in starImports) {
            val candidateFqn = "${starPackage.asString()}.$simpleName"
            if (tryResolve(candidateFqn)) {
                if (foundFqn != null) return null // Ambiguous
                foundFqn = candidateFqn
            }
        }
        if (foundFqn != null) return foundFqn
        
        // 4. Nested classes of supertypes (JLS 6.5.2 - inherited member types)
        // In Java, nested classes of supertypes are in scope. For example, if class C implements
        // interface I, and I has nested class I.X, then X can be referenced as just "X" inside C.
        // For nested inner classes, we also need to check outer classes' supertypes.
        var containingClass = containingClassProvider?.invoke()
        while (containingClass != null) {
            foundFqn = resolveFromSupertypes(simpleName, containingClass, tryResolve)
            if (foundFqn != null) return foundFqn
            containingClass = containingClass.outerClass
        }
        
        return foundFqn
    }
    
    /**
     * Try to resolve a simple name as a nested class of one of the supertypes.
     * This implements JLS 6.5.2 - inherited member types are in scope.
     * Returns null if ambiguous (multiple supertypes have nested class with same name).
     */
    private fun resolveFromSupertypes(simpleName: String, containingClass: JavaClass, tryResolve: (String) -> Boolean): String? {
        val visited = mutableSetOf<String>()
        val allMatches = mutableSetOf<String>()
        resolveFromSupertypesRecursive(simpleName, containingClass, tryResolve, visited, allMatches)
        // Return null if ambiguous (multiple different fully qualified names found)
        return if (allMatches.size > 1) null else allMatches.firstOrNull()
    }
    
    private fun resolveFromSupertypesRecursive(
        simpleName: String, 
        javaClass: JavaClass, 
        tryResolve: (String) -> Boolean,
        visited: MutableSet<String>,
        allMatches: MutableSet<String>
    ) {
        for (supertype in javaClass.supertypes) {
            var supertypeName = supertype.classifierQualifiedName
            if (supertypeName in visited) continue
            visited.add(supertypeName)
            
            // If supertypeName is not fully qualified (no dots), try with package prefix
            // This handles same-package supertypes where classifierQualifiedName returns just "x"
            if (!supertypeName.contains('.') && !packageFqName.isRoot) {
                val packageQualified = "${packageFqName.asString()}.$supertypeName"
                // Try package-qualified version first
                val nestedCandidate = "$packageQualified.$simpleName"
                if (tryResolve(nestedCandidate)) {
                    allMatches.add(nestedCandidate)
                }
                // If that worked for the supertype itself, use it for recursion
                if (tryResolve(packageQualified)) {
                    supertypeName = packageQualified
                }
            }
            
            // Try the simple name as a nested class of this supertype
            // e.g., if supertype is "a.x" and simpleName is "y", try "a.x.y"
            val nestedCandidate = "$supertypeName.$simpleName"
            if (tryResolve(nestedCandidate)) {
                allMatches.add(nestedCandidate)
            }
            
            // Also recursively check supertypes of this supertype
            // This requires resolving the supertype to a JavaClass, which we can only do
            // if it's in the local scope
            val supertypeClass = supertype.classifier as? JavaClass
            if (supertypeClass != null) {
                resolveFromSupertypesRecursive(simpleName, supertypeClass, tryResolve, visited, allMatches)
            }
        }
    }

    companion object {
        fun create(root: JavaSyntaxNode): JavaResolutionContext {
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
                localClassProvider = localClassProvider
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
                    simpleImports[fqName.substringAfterLast('.')] = FqName(fqName)
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
                    simpleImports[identifiers.last()] = FqName(fqName)
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
                                simpleImports[fqName.substringAfterLast('.')] = FqName(fqName)
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
