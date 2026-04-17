/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.name.FqName
import java.util.WeakHashMap
import java.util.Collections

/**
 * Handles extraction and lookup of Java import declarations from AST nodes.
 *
 * Responsible for:
 * - Parsing import statements (normal, static, error-recovery, fragmented) from the AST
 * - Extracting the package name from a compilation unit
 * - Finding top-level class nodes by name
 *
 * The [extractImports] result is cached per root [JavaSyntaxNode] via a weak-keyed map so
 * repeated context creations over the same compilation unit do not re-walk the AST.
 */
internal object JavaImportResolver {

    /**
     * Weak-keyed cache of extracted import data per compilation-unit root node.
     * Entries are evicted automatically when the root `JavaSyntaxNode` becomes unreachable.
     * Wrapped in [Collections.synchronizedMap] because resolution contexts may be created
     * from multiple threads during FIR analysis.
     */
    private val importCache: MutableMap<JavaSyntaxNode, Pair<Map<String, FqName>, List<FqName>>> =
        Collections.synchronizedMap(WeakHashMap())

    /**
     * Extracts the package name from a compilation unit root node.
     */
    fun extractPackageName(root: JavaSyntaxNode): FqName {
        val packageStmt = root.findChildByType(JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = packageStmt?.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.text
        return if (packageName != null) FqName(packageName) else FqName.ROOT
    }

    /**
     * Extracts all import declarations from a compilation unit root node.
     *
     * Returns a pair of:
     * - Simple (single-type) imports: map from simple name to fully qualified name
     * - Star (on-demand) imports: list of package FqNames
     *
     * Handles four import patterns:
     * 1. Normal imports (`import pkg.Class`)
     * 2. Static imports (`import static pkg.Class.MEMBER`)
     * 3. Error-element imports (parser recovery for reserved-word packages like `kotlin`)
     * 4. Fragmented imports (parser splits import across sibling nodes)
     */
    fun extractImports(root: JavaSyntaxNode): Pair<Map<String, FqName>, List<FqName>> {
        importCache[root]?.let { return it }
        val result = extractImportsUncached(root)
        importCache[root] = result
        return result
    }

    private fun extractImportsUncached(root: JavaSyntaxNode): Pair<Map<String, FqName>, List<FqName>> {
        val simpleImports = mutableMapOf<String, FqName>()
        val starImports = mutableListOf<FqName>()

        // Handle case where root might be CLASS instead of compilation unit
        val importList = root.findChildByType(JavaSyntaxElementType.IMPORT_LIST)
            ?: root.findChildByType(JavaSyntaxElementType.CLASS)?.findChildByType(JavaSyntaxElementType.IMPORT_LIST)

        if (importList != null) {
            extractNormalImports(importList, simpleImports, starImports)
            extractStaticImports(importList, simpleImports, starImports)
            extractErrorElementImports(importList, simpleImports, starImports)
        }

        // Fast path: fragmented imports only occur when the parser emits ERROR_ELEMENT children
        // at the root level. For well-formed files (the common case) there are none, so we can
        // skip walking `root.children` entirely — that loop is O(N) in the file size otherwise.
        if (root.children.any { it.type == SyntaxTokenTypes.ERROR_ELEMENT }) {
            extractFragmentedImports(root, simpleImports, starImports)
        }

        return simpleImports to starImports
    }

    /**
     * Pattern 1: well-formed `import pkg.Class;` / `import pkg.*;` statements.
     */
    private fun extractNormalImports(
        importList: JavaSyntaxNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (importNode in importList.getChildrenByType(JavaSyntaxElementType.IMPORT_STATEMENT)) {
            val codeRef = importNode.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: continue
            val hasStar = importNode.children.any { it.type == JavaSyntaxTokenType.ASTERISK }
            val fqName = codeRef.text

            if (hasStar) {
                starImports.add(FqName(fqName))
            } else {
                // Keep first occurrence: duplicate explicit imports for the same simple name
                // are a compile error in Java. PSI uses first-seen semantics, so we do too.
                val simpleName = fqName.substringAfterLast('.')
                simpleImports.putIfAbsent(simpleName, FqName(fqName))
            }
        }
    }

    /**
     * Pattern 2: `import static pkg.Class.MEMBER;` / `import static pkg.Class.*;`.
     *
     * The KMP parser uses IMPORT_STATIC_STATEMENT with IMPORT_STATIC_REFERENCE child.
     */
    private fun extractStaticImports(
        importList: JavaSyntaxNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (importNode in importList.getChildrenByType(JavaSyntaxElementType.IMPORT_STATIC_STATEMENT)) {
            val refNode = importNode.findChildByType(JavaSyntaxElementType.IMPORT_STATIC_REFERENCE) ?: continue
            val hasStar = importNode.children.any { it.type == JavaSyntaxTokenType.ASTERISK }
            val fqName = refNode.text

            if (hasStar) {
                starImports.add(FqName(fqName))
            } else {
                // e.g. "example.KotlinDtoMapping.ID" → simpleName = "ID"
                val simpleName = fqName.substringAfterLast('.')
                simpleImports.putIfAbsent(simpleName, FqName(fqName))
            }
        }
    }

    /**
     * Pattern 3: ERROR_ELEMENT inside IMPORT_LIST. The parser emits these when the import
     * starts with a reserved word (e.g. `import kotlin.X;`) — the overall IMPORT_LIST is
     * intact but one entry became an ERROR_ELEMENT with recoverable IDENTIFIER/DOT children.
     */
    private fun extractErrorElementImports(
        importList: JavaSyntaxNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (errorNode in importList.getChildrenByType(SyntaxTokenTypes.ERROR_ELEMENT)) {
            if (errorNode.findChildByType(JavaSyntaxTokenType.IMPORT_KEYWORD) == null) continue

            // Reconstruct the import from IDENTIFIER and DOT children
            val identifiers = mutableListOf<String>()
            for (child in errorNode.children) {
                if (child.type == JavaSyntaxTokenType.IDENTIFIER) {
                    identifiers.add(child.text)
                }
            }
            if (identifiers.isEmpty()) continue

            val hasStar = errorNode.children.any { it.type == JavaSyntaxTokenType.ASTERISK }
            val fqName = identifiers.joinToString(".")

            if (hasStar) {
                starImports.add(FqName(fqName))
            } else {
                simpleImports.putIfAbsent(identifiers.last(), FqName(fqName))
            }
        }
    }

    /**
     * Pattern 4: fragmented imports — the parser has split the import across sibling nodes
     * at the compilation-unit root.
     *
     * Shape 1: `ERROR_ELEMENT(IMPORT_KEYWORD)` followed by `TYPE(JAVA_CODE_REFERENCE)` — simple import.
     * Shape 2: `ERROR_ELEMENT(import)` followed by `TYPE(pkg.)` followed by `ERROR_ELEMENT(*;)` — star import.
     *
     * The parser may insert MODIFIER_LIST and whitespace between the anchor and the type node.
     */
    private fun extractFragmentedImports(
        root: JavaSyntaxNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        val allChildren = root.children
        var i = 0
        while (i < allChildren.size) {
            val node = allChildren[i]

            // Check for ERROR_ELEMENT containing "import" keyword or text
            val isImportError = node.type == SyntaxTokenTypes.ERROR_ELEMENT &&
                    (node.findChildByType(JavaSyntaxTokenType.IMPORT_KEYWORD) != null || node.text.trim() == "import")

            if (isImportError) {
                // Look for the next TYPE or JAVA_CODE_REFERENCE sibling, skipping whitespace and modifier list
                var typeNode: JavaSyntaxNode? = null
                var hasStar = false

                for (j in (i + 1) until allChildren.size) {
                    val sibling = allChildren[j]
                    // Skip whitespace, empty modifier lists, and empty error elements
                    if (sibling.type == SyntaxTokenTypes.WHITE_SPACE || sibling.type == JavaSyntaxElementType.MODIFIER_LIST) continue
                    if (sibling.type == SyntaxTokenTypes.ERROR_ELEMENT && sibling.text.isBlank()) continue

                    if (sibling.type == JavaSyntaxElementType.TYPE || sibling.type == JavaSyntaxElementType.JAVA_CODE_REFERENCE) {
                        typeNode = sibling
                        // Continue to check for star in following siblings (not just the next one)
                        for (k in (j + 1) until allChildren.size) {
                            val nextSib = allChildren[k]
                            if (nextSib.type == SyntaxTokenTypes.WHITE_SPACE || nextSib.type == JavaSyntaxElementType.MODIFIER_LIST) continue
                            if (nextSib.type == SyntaxTokenTypes.ERROR_ELEMENT && nextSib.text.isBlank()) continue
                            if (nextSib.type == SyntaxTokenTypes.ERROR_ELEMENT && nextSib.text.contains("*")) {
                                hasStar = true
                                break
                            }
                            // Stop at CLASS or other significant nodes (interfaces/enums are also CLASS nodes)
                            if (nextSib.type == JavaSyntaxElementType.CLASS) break
                        }
                        break
                    }
                    // Also check if ERROR_ELEMENT itself contains star (like "*;")
                    if (sibling.type == SyntaxTokenTypes.ERROR_ELEMENT && sibling.text.contains("*")) {
                        hasStar = true
                    }
                    // Stop at CLASS or other significant nodes (interfaces/enums are also CLASS nodes)
                    if (sibling.type == JavaSyntaxElementType.CLASS) break
                }

                if (typeNode != null) {
                    val ref = typeNode.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: typeNode
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
                            simpleImports.putIfAbsent(simpleName, FqName(fqName))
                        }
                    }
                }
            }
            i++
        }
    }

    /**
     * Finds a top-level class node by name in the compilation unit root.
     */
    fun findClassNode(root: JavaSyntaxNode, name: org.jetbrains.kotlin.name.Name): JavaSyntaxNode? {
        for (child in root.children) {
            if (child.type == JavaSyntaxElementType.CLASS) {
                val id = child.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text
                if (id == name.asString()) return child
            }
        }
        return null
    }
}
