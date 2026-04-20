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
 * The [extractImports] result is cached per [JavaLightTree] via a weak-keyed map so repeated
 * context creations over the same compilation unit do not re-walk the AST.
 */
internal object JavaImportResolver {

    /**
     * Weak-keyed cache of extracted import data per compilation-unit light tree.
     * Entries are evicted automatically when the [JavaLightTree] becomes unreachable.
     * Wrapped in [Collections.synchronizedMap] because resolution contexts may be created
     * from multiple threads during FIR analysis.
     */
    private val importCache: MutableMap<JavaLightTree, Pair<Map<String, FqName>, List<FqName>>> =
        Collections.synchronizedMap(WeakHashMap())

    /**
     * Extracts the package name from a compilation unit root node.
     */
    fun extractPackageName(tree: JavaLightTree, root: JavaLightNode): FqName {
        val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = packageStmt?.let {
            tree.findChildByType(it, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        }
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
    fun extractImports(tree: JavaLightTree, root: JavaLightNode): Pair<Map<String, FqName>, List<FqName>> {
        importCache[tree]?.let { return it }
        val result = extractImportsUncached(tree, root)
        importCache[tree] = result
        return result
    }

    private fun extractImportsUncached(tree: JavaLightTree, root: JavaLightNode): Pair<Map<String, FqName>, List<FqName>> {
        val simpleImports = mutableMapOf<String, FqName>()
        val starImports = mutableListOf<FqName>()

        val importList = tree.findChildByType(root, JavaSyntaxElementType.IMPORT_LIST)
            ?: tree.findChildByType(root, JavaSyntaxElementType.CLASS)?.let { tree.findChildByType(it, JavaSyntaxElementType.IMPORT_LIST) }

        if (importList != null) {
            extractNormalImports(tree, importList, simpleImports, starImports)
            extractStaticImports(tree, importList, simpleImports, starImports)
            extractErrorElementImports(tree, importList, simpleImports, starImports)
        }

        // Fast path: fragmented imports only occur when the parser emits ERROR_ELEMENT children
        // at the root level. For well-formed files (the common case) there are none, so we can
        // skip walking `root.children` entirely.
        if (tree.getChildren(root).any { tree.getType(it) == SyntaxTokenTypes.ERROR_ELEMENT }) {
            extractFragmentedImports(tree, root, simpleImports, starImports)
        }

        return simpleImports to starImports
    }

    /**
     * Pattern 1: well-formed `import pkg.Class;` / `import pkg.*;` statements.
     */
    private fun extractNormalImports(
        tree: JavaLightTree,
        importList: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (importNode in tree.getChildrenByType(importList, JavaSyntaxElementType.IMPORT_STATEMENT)) {
            val codeRef = tree.findChildByType(importNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: continue
            val hasStar = tree.getChildren(importNode).any { tree.getType(it) == JavaSyntaxTokenType.ASTERISK }
            val fqName = tree.getText(codeRef).toString()

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
        tree: JavaLightTree,
        importList: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (importNode in tree.getChildrenByType(importList, JavaSyntaxElementType.IMPORT_STATIC_STATEMENT)) {
            val refNode = tree.findChildByType(importNode, JavaSyntaxElementType.IMPORT_STATIC_REFERENCE) ?: continue
            val hasStar = tree.getChildren(importNode).any { tree.getType(it) == JavaSyntaxTokenType.ASTERISK }
            val fqName = tree.getText(refNode).toString()

            if (hasStar) {
                starImports.add(FqName(fqName))
            } else {
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
        tree: JavaLightTree,
        importList: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (errorNode in tree.getChildrenByType(importList, SyntaxTokenTypes.ERROR_ELEMENT)) {
            if (tree.findChildByType(errorNode, JavaSyntaxTokenType.IMPORT_KEYWORD) == null) continue

            // Reconstruct the import from IDENTIFIER and DOT children
            val identifiers = mutableListOf<String>()
            for (child in tree.getChildren(errorNode)) {
                if (tree.getType(child) == JavaSyntaxTokenType.IDENTIFIER) {
                    identifiers.add(tree.getText(child).toString())
                }
            }
            if (identifiers.isEmpty()) continue

            val hasStar = tree.getChildren(errorNode).any { tree.getType(it) == JavaSyntaxTokenType.ASTERISK }
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
        tree: JavaLightTree,
        root: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        val allChildren = tree.getChildren(root)
        var i = 0
        while (i < allChildren.size) {
            val node = allChildren[i]
            val nodeType = tree.getType(node)

            val isImportError = nodeType == SyntaxTokenTypes.ERROR_ELEMENT &&
                    (tree.findChildByType(node, JavaSyntaxTokenType.IMPORT_KEYWORD) != null ||
                            tree.getText(node).toString().trim() == "import")

            if (isImportError) {
                var typeNode: JavaLightNode? = null
                var hasStar = false

                for (j in (i + 1) until allChildren.size) {
                    val sibling = allChildren[j]
                    val siblingType = tree.getType(sibling)
                    if (siblingType == SyntaxTokenTypes.WHITE_SPACE || siblingType == JavaSyntaxElementType.MODIFIER_LIST) continue
                    if (siblingType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(sibling).toString().isBlank()) continue

                    if (siblingType == JavaSyntaxElementType.TYPE || siblingType == JavaSyntaxElementType.JAVA_CODE_REFERENCE) {
                        typeNode = sibling
                        for (k in (j + 1) until allChildren.size) {
                            val nextSib = allChildren[k]
                            val nextType = tree.getType(nextSib)
                            if (nextType == SyntaxTokenTypes.WHITE_SPACE || nextType == JavaSyntaxElementType.MODIFIER_LIST) continue
                            if (nextType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(nextSib).toString().isBlank()) continue
                            if (nextType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(nextSib).toString().contains("*")) {
                                hasStar = true
                                break
                            }
                            if (nextType == JavaSyntaxElementType.CLASS) break
                        }
                        break
                    }
                    if (siblingType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(sibling).toString().contains("*")) {
                        hasStar = true
                    }
                    if (siblingType == JavaSyntaxElementType.CLASS) break
                }

                if (typeNode != null) {
                    val ref = tree.findChildByType(typeNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: typeNode
                    var fqName = tree.getText(ref).toString().trim()
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
    fun findClassNode(tree: JavaLightTree, root: JavaLightNode, name: org.jetbrains.kotlin.name.Name): JavaLightNode? {
        for (child in tree.getChildren(root)) {
            if (tree.getType(child) == JavaSyntaxElementType.CLASS) {
                val idNode = tree.findChildByType(child, JavaSyntaxTokenType.IDENTIFIER) ?: continue
                if (tree.textEquals(idNode, name.asString())) return child
            }
        }
        return null
    }
}
