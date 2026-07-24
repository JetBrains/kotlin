/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.LeanJavaClassFinder
import org.jetbrains.kotlin.java.direct.util.DefaultJavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import java.nio.file.Path

/**
 * Shared local VFS used by the tests below to convert `@TempDir` paths into [VirtualFile]s
 * that `JavaClassFinderOverAstImpl` and `extractFileInfoLightweight` now consume. The instance
 * is stateless (no VFS refresh) so reusing it across tests is safe.
 */
internal val testLocalFs = KotlinLocalFileSystem()

internal fun Path.toVirtualFile(): VirtualFile =
    testLocalFs.findFileByNioFile(this)
        ?: error("Could not obtain VirtualFile for path: $this (does it exist?)")

/**
 * Light-tree snapshot used by tests that need direct AST navigation.
 *
 * Destructuring order is `(root, context, tree)`. The owning [tree] is available via the third
 * component or the [tree] property for tests that need direct AST navigation.
 */
data class ParsedSource(
    val root: JavaLightNode,
    val context: JavaResolutionContext,
    val tree: JavaLightTree,
)

open class JavaParsingTestBase {

    protected fun parseSource(source: String): ParsedSource {
        val tree = parseJavaToLightTree(source, 0)
        lateinit var context: JavaResolutionContext
        val classFinder = SameFileOnlyClassFinder { context }
        context = JavaResolutionContext.create(tree, session = createDummyFirSessionForTests(), classFinder = classFinder)
        return ParsedSource(tree.getRoot(), context, tree)
    }

    protected fun parseFirstClass(source: String): JavaClassOverAst {
        val parsed = parseSource(source)
        val classNode = parsed.tree.getChildren(parsed.root).first {
            parsed.tree.getType(it).toString() == "CLASS"
        }
        return JavaClassOverAst(classNode, parsed.tree, parsed.context)
    }
}

/**
 * [LeanJavaClassFinder] restricted to top-level classes declared in the same file as [context].
 * Lets unit tests exercise the module's real inherited-inner-class resolution path
 * ([org.jetbrains.kotlin.java.direct.resolution.resolveInheritedInnerClassToClassId])
 * for same-file supertypes without a full [JavaClassFinderOverAstImpl] backed by real files.
 *
 * [context] is passed as a supplier because it isn't constructed yet when this finder is built
 * (`JavaResolutionContext.create` needs the finder as a constructor argument).
 */
private class SameFileOnlyClassFinder(private val context: () -> JavaResolutionContext) : LeanJavaClassFinder {
    override fun isClassInIndex(classId: ClassId): Boolean = findClass(JavaClassFinder.Request(classId)) != null

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        val resolutionContext = context()
        if (request.classId.packageFqName != resolutionContext.packageFqName) return null
        val segments = request.classId.relativeClassName.pathSegments()
        var current: JavaClass = resolutionContext.scopeContext.sameFileTopLevelClassProvider(segments.first()) ?: return null
        for (i in 1 until segments.size) {
            current = current.findInnerClass(segments[i]) ?: return null
        }
        return current
    }

    override fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> = emptyMap()

    override fun getDirectSupertypes(classId: ClassId): List<ClassId> = emptyList()
}

/**
 * Test-only [JavaClassFinderOverAstImpl] factory that supplies a dummy source-kind [FirSession].
 */
internal fun JavaClassFinderOverAstImpl(
    sourceRoots: List<VirtualFile>,
    sourceFileReader: JavaSourceFileReader = DefaultJavaSourceFileReader,
): JavaClassFinderOverAstImpl =
    JavaClassFinderOverAstImpl(
        createDummyFirSessionForTests(),
        JavaSourceRootEntry.fromRootsWithoutPrefix(sourceRoots),
        sourceFileReader,
    )

/**
 * Constructs a minimal [FirSession] with no registered components, intended only for parsing-level
 * unit tests of the `java-direct` module.
 */
internal fun createDummyFirSessionForTests(): FirSession =
    DummyJavaDirectFirSession(FirSession.Kind.Source)

@OptIn(PrivateSessionConstructor::class)
private class DummyJavaDirectFirSession(kind: Kind) : FirSession(kind)

