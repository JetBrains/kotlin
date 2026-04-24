/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

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

    operator fun set(classId: ClassId, javaClass: JavaClass) {
        classCache[classId] = javaClass
    }

    @OptIn(ExperimentalContracts::class)
    fun getOrPutIfNotNull(classId: ClassId, makeClass: () -> JavaClass?): JavaClass? {
        contract { callsInPlace(makeClass, InvocationKind.AT_MOST_ONCE) }
        return classCache[classId] ?: makeClass()?.also { classCache[classId] = it }
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
            if (cid !in classCache) {
                val javaClass = resolutionContext.findLocalClass(Name.identifier(className))
                if (javaClass != null) {
                    classCache[cid] = javaClass
                }
            }
        }

        return classCache[classId] as? JavaClassOverAst
    }
}
