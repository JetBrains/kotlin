/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * [ClassId] → [JavaClass] memoization plus the file-parse entry point.
 *
 * [parseTopLevelClassFromFile] always populates the cache for **all** top-level classes declared
 * in the parsed file, not just the requested one. This ensures FIR sees the same
 * [JavaClassOverAst] (and therefore the same [JavaTypeParameterOverAst] instances) regardless of
 * whether a class was looked up directly or reached via inner-class navigation — FIR matches
 * Java type parameters by object identity.
 */
internal class JavaClassCache(
    private val sourceFileReader: JavaSourceFileReader,
    private val resolutionContextFactory: (JavaLightTree) -> JavaResolutionContext,
) {
    private val classCache: MutableMap<ClassId, JavaClass> = ConcurrentHashMap()

    operator fun get(classId: ClassId): JavaClass? = classCache[classId]

    @OptIn(ExperimentalContracts::class)
    fun getOrPutIfNotNull(classId: ClassId, makeClass: () -> JavaClass?): JavaClass? {
        contract { callsInPlace(makeClass, InvocationKind.AT_MOST_ONCE) }
        classCache[classId]?.let { return it }
        val made = makeClass() ?: return null
        // putIfAbsent returns the previously associated value (the winner) or null if this thread
        // installed the entry. Returning the winner preserves the by-identity contract.
        return classCache.putIfAbsent(classId, made) ?: made
    }

    fun parseTopLevelClassFromFile(fileEntry: JavaPackageIndexer.FileEntry, simpleName: String): JavaClassOverAst? {
        // Identity fast-path: all lookups (direct or via inner-class navigation) must return the
        // *same* JavaClassOverAst for a given top-level class.
        val classId = ClassId(fileEntry.packageFqName, FqName(simpleName), isLocal = false)
        classCache[classId]?.let { return it as? JavaClassOverAst }

        val source = sourceFileReader.readFileContent(fileEntry.file) ?: return null
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()
        val resolutionContext = resolutionContextFactory(tree)

        // Cache ALL top-level classes from this file to avoid reparsing for sibling classes.
        val allClassNames = tree.getChildrenByType(root, JavaSyntaxElementType.CLASS).mapNotNull {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() }
        }
        for (className in allClassNames) {
            val cid = ClassId(fileEntry.packageFqName, FqName(className), isLocal = false)
            if (classCache[cid] != null) continue
            // This resolutionContext is always freshly created for a top-level parse, so its
            // scopeContext.containingClass is always null — no containing-class chain to search,
            // just the top-level classes declared in this file.
            val javaClass = resolutionContext.scopeContext.sameFileTopLevelClassProvider(Name.identifier(className)) ?: continue
            // putIfAbsent: if a concurrent thread already installed an entry, drop ours and keep
            // theirs so all callers observe the same JavaClassOverAst instance (identity contract).
            classCache.putIfAbsent(cid, javaClass)
        }

        return classCache[classId] as? JavaClassOverAst
    }
}
