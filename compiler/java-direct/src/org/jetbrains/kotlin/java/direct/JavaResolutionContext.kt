/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolution context for Java source files. Encapsulates all information
 * needed to resolve type references within a compilation unit.
 *
 * This is analogous to FIR scopes but simplified for Java's scoping rules.
 */
class JavaResolutionContext private constructor(
    val source: CharSequence,
    val packageFqName: FqName,
    private val simpleImports: Map<String, FqName>,
    private val starImports: List<FqName>,
    private val localClassProvider: (Name) -> JavaClass?,
) {
    fun findLocalClass(name: Name): JavaClass? = localClassProvider(name)

    fun getSimpleImport(simpleName: String): FqName? = simpleImports[simpleName]

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
        fun create(root: JavaSyntaxNode, source: CharSequence): JavaResolutionContext {
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
                source = source,
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

            val importList = root.findChildByType("IMPORT_LIST")
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
