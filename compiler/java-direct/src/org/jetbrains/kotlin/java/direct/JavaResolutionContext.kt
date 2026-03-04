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
     * 2. Top-level classes in the same compilation unit
     */
    fun findLocalClass(name: Name): JavaClass? {
        // First check inner classes of the containing class
        containingClassProvider?.invoke()?.findInnerClass(name)?.let { return it }
        // Then check top-level classes
        return localClassProvider(name)
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
     * Resolve a simple type name using the callback for external resolution.
     * Called by [JavaClassifierTypeOverAst.resolve] when FIR needs to resolve star imports.
     *
     * Resolution order (per JLS):
     * 1. Same package
     * 2. java.lang.* (automatic import)
     * 3. Explicit star imports
     */
    fun resolveWithCallback(simpleName: String, tryResolve: (String) -> Boolean): String? {
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
        return foundFqn
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
            // Pattern: ERROR_ELEMENT(IMPORT_KEYWORD) followed eventually by TYPE(JAVA_CODE_REFERENCE)
            // Parser may insert MODIFIER_LIST and other nodes between them
            val allChildren = root.children
            var i = 0
            while (i < allChildren.size) {
                val node = allChildren[i]
                if (node.type.toString() == "ERROR_ELEMENT" && 
                    node.findChildByType("IMPORT_KEYWORD") != null) {
                    // Look for the next TYPE or JAVA_CODE_REFERENCE sibling, skipping whitespace and modifier list
                    for (j in (i + 1) until allChildren.size) {
                        val sibling = allChildren[j]
                        val sibType = sibling.type.toString()
                        // Skip whitespace and empty modifier lists
                        if (sibType == "WHITE_SPACE" || sibType == "MODIFIER_LIST") continue
                        if (sibType == "TYPE" || sibType == "JAVA_CODE_REFERENCE") {
                            val ref = sibling.findChildByType("JAVA_CODE_REFERENCE") ?: sibling
                            val fqName = ref.text.trim()
                            if (fqName.contains('.')) {
                                simpleImports[fqName.substringAfterLast('.')] = FqName(fqName)
                            }
                        }
                        break
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
