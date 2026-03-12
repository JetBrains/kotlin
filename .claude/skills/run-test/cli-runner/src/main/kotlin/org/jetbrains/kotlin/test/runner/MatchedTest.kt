@file:Suppress("MatchingDeclarationName")

package org.jetbrains.kotlin.test.runner

import java.io.File
import java.nio.file.Path

data class MatchedTest(
    val className: String,
    val methodName: String?,
    val classFilePath: Path,
)

private class MatchingContext(
    testDataPath: Path,
    projectRoot: Path,
    val scannedClasses: Map<String, ScannedClass>,
    val verbose: Boolean,
) {
    val projectRootStr: String = projectRoot.toFile().canonicalPath
    val targetFile: File = testDataPath.toFile().canonicalFile
    val targetName: String = targetFile.name.toNormalizedTestName()
    val targetMethodName: String = "test$targetName"
    val targetPathNormalized: String = targetFile.absolutePath.asPathWithoutAllExtensions
}

fun findMatchingTests(
    testDataPath: Path,
    projectRoot: Path,
    scannedClasses: Map<String, ScannedClass>,
    verbose: Boolean = false,
): List<MatchedTest> {
    val ctx = MatchingContext(testDataPath, projectRoot, scannedClasses, verbose)

    if (verbose) {
        println("[Matcher] Target file: ${ctx.targetFile}")
        println("[Matcher] Normalized name: ${ctx.targetName}")
        println("[Matcher] Looking for method: ${ctx.targetMethodName}")
    }

    val methodMatches = findMethodLevelMatches(ctx)
    val directoryMatches = findDirectoryLevelMatches(ctx)

    return (methodMatches + directoryMatches).distinctBy { it.className to it.methodName }
}

private fun findMethodLevelMatches(ctx: MatchingContext): List<MatchedTest> {
    val results = mutableListOf<MatchedTest>()
    for ((_, scannedClass) in ctx.scannedClasses) {
        val matchingMethods =
            scannedClass.methods.filter {
                it.name == ctx.targetMethodName && it.testMetadataValue != null
            }
        for (method in matchingMethods) {
            val match = tryMatchMethod(ctx, scannedClass, method)
            if (match != null) results.add(match)
        }
    }
    return results
}

private fun tryMatchMethod(
    ctx: MatchingContext,
    scannedClass: ScannedClass,
    method: ScannedMethod,
): MatchedTest? {
    val candidatePath =
        buildResolvedPath(
            method.testMetadataValue,
            scannedClass,
            ctx.scannedClasses,
            ctx.projectRootStr,
        )
    if (ctx.verbose) {
        println("[Matcher] Candidate: ${scannedClass.className}#${method.name} -> $candidatePath")
    }
    if (candidatePath != ctx.targetPathNormalized) return null
    if (ctx.verbose) println("[Matcher]   MATCHED!")
    return MatchedTest(scannedClass.className.replace('/', '.'), method.name, scannedClass.classFilePath)
}

private fun findDirectoryLevelMatches(ctx: MatchingContext): List<MatchedTest> {
    val targetDirPath =
        ctx.targetFile.parentFile
            ?.absolutePath
            ?.asPathWithoutAllExtensions ?: return emptyList()
    val results = mutableListOf<MatchedTest>()
    for ((_, scannedClass) in ctx.scannedClasses) {
        val match = tryMatchDirectory(ctx, scannedClass, targetDirPath)
        if (match != null) results.add(match)
    }
    return results
}

private fun tryMatchDirectory(
    ctx: MatchingContext,
    scannedClass: ScannedClass,
    targetDirPath: String,
): MatchedTest? {
    val simpleClassName =
        scannedClass.className
            .substringAfterLast('/')
            .substringAfterLast('$')
    if (simpleClassName != ctx.targetName || scannedClass.testMetadataValue == null) {
        return null
    }

    val candidatePath =
        buildResolvedPath(
            null,
            scannedClass,
            ctx.scannedClasses,
            ctx.projectRootStr,
        )
    if (ctx.verbose) {
        println("[Matcher] Directory candidate: ${scannedClass.className} -> $candidatePath")
    }
    val matches =
        candidatePath == targetDirPath ||
            candidatePath == ctx.targetPathNormalized
    if (ctx.verbose && matches) println("[Matcher]   MATCHED (directory-level)!")
    return if (matches) {
        MatchedTest(
            scannedClass.className.replace('/', '.'),
            null,
            scannedClass.classFilePath,
        )
    } else {
        null
    }
}

private fun buildResolvedPath(
    methodPathPart: String?,
    scannedClass: ScannedClass,
    scannedClasses: Map<String, ScannedClass>,
    projectRoot: String,
): String {
    val (testMetadata, testDataPath) = collectClassHierarchyMetadata(scannedClass, scannedClasses)
    val resolvedTestDataPath = testDataPath?.replace("\$PROJECT_ROOT", projectRoot)

    val parts =
        buildList {
            methodPathPart?.let(::add)
            testMetadata?.takeIf(String::isNotEmpty)?.let(::add)
            resolvedTestDataPath?.takeIf(String::isNotEmpty)?.let(::add)
            if (resolvedTestDataPath == null) add(projectRoot)
        }

    return File(parts.reversed().joinToString("/"))
        .canonicalFile.absolutePath.asPathWithoutAllExtensions
}

private fun collectClassHierarchyMetadata(
    scannedClass: ScannedClass,
    scannedClasses: Map<String, ScannedClass>,
): Pair<String?, String?> {
    var testMetadata: String? = scannedClass.testMetadataValue
    var testDataPath: String? = scannedClass.testDataPathValue

    var current = scannedClass
    while (current.outerClassName != null) {
        val outer = scannedClasses[current.outerClassName] ?: break
        testMetadata = prependOuterMetadata(outer.testMetadataValue, testMetadata)
        if (outer.testDataPathValue != null) testDataPath = outer.testDataPathValue
        current = outer
    }

    return testMetadata to testDataPath
}

private fun prependOuterMetadata(
    outerValue: String?,
    current: String?,
): String? =
    when {
        outerValue.isNullOrEmpty() -> current
        current.isNullOrEmpty() -> outerValue
        else -> "$outerValue/$current"
    }
