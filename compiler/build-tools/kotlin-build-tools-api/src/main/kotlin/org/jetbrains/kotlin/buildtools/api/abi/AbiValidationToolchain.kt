/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.abi

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationCompareDumpFormatV2
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteJvmDumpFormatV2
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteKlibDumpFormatV2
import java.io.File

/**
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface AbiValidationToolchain {
    public fun writeJvmDumpFormatV2(
        appendable: Appendable,
        inputFiles: Iterable<File>
    ): AbiValidationWriteJvmDumpFormatV2

    public fun writeKlibDumpFormatV2(
        appendable: Appendable,
        referenceDumpFile: File,
        klibs: Map<KlibTargetId, File>,
        unsupported: Set<KlibTargetId>
    ): AbiValidationWriteKlibDumpFormatV2

    public fun findDiffFormatV2(expectedDumpFile: File, actualDumpFile: File): AbiValidationCompareDumpFormatV2
}

/**
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public class AbiFilters(
    /**
     * Include class into dump by its name.
     * Classes that do not match the specified names, that do not have an annotation from [includedAnnotatedWith]
     * and do not have members marked with an annotation from [includedAnnotatedWith] are excluded from the dump.
     *
     * The name filter compares the qualified class name with the value in the filter:
     *
     * For Kotlin classes, fully qualified names are used.
     * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
     * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     */
    public val includedClasses: Set<String> = emptySet(),

    /**
     * Excludes a class from a dump by its name.
     *
     * The name filter compares the qualified class name with the value in the filter:
     *
     * For Kotlin classes, fully qualified names are used.
     * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
     * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     */
    public val excludedClasses: Set<String> = emptySet(),

    /**
     * Includes a declaration by annotations placed on it.
     *
     * Any declaration that is not marked with one of the these annotations and does not match the [includedClasses] is excluded from the dump.
     *
     * The declaration can be a class, a class member (function or property), a top-level function or a top-level property.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     *
     * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
     */
    public val includedAnnotatedWith: Set<String> = emptySet(),

    /**
     * Excludes a declaration by annotations placed on it.
     *
     * It means that a class, a class member (function or property), a top-level function or a top-level property
     * marked by a specific annotation will be excluded from the dump.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     *
     * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
     */
    public val excludedAnnotatedWith: Set<String> = emptySet(),
) {
    public companion object {
        public val EMPTY: AbiFilters = AbiFilters()
    }
}

/**
 *
 * @since 2.3.0
 */
public class KlibTargetId(
    /**
     * An actual name of a target that remains unaffected by any custom settings.
     */
    public val targetName: String,
    /**
     * A name of a target that could be configured by a user.
     * Usually, it's the same name as [targetName].
     */
    public val configurableName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KlibTargetId) return false

        if (targetName != other.targetName) return false
        if (configurableName != other.configurableName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = targetName.hashCode()
        result = 31 * result + configurableName.hashCode()
        return result
    }
}
/*
@ExperimentalBuildToolsApi
public class AbiValidationToolchainImpl(abiTools: AbiToolsV2) : AbiValidationToolchain {

    public override fun writeJvmDumpFormatV2(appendable: Appendable, inputFiles: Iterable<File>, filters: AbiFilters)
//        tools.v2.printJvmDump(writer, classfiles, filters)

    public override fun writeKlibDump(
        referenceDump: File,
        klibs: Map<KlibTargetId, File>,
        unsupported: Set<KlibTargetId>,
        filters: AbiFilters,
    )
//        val mergedDump = tools.v2.createKlibDump()
//        klibTargets.forEach { suite ->
//            val klibDir = suite.klibFiles.files.first()
//            if (klibDir.exists()) {
//                val dump = tools.v2.extractKlibAbi(klibDir, KlibTarget(suite.canonicalTargetName, suite.targetName), filters)
//                mergedDump.merge(dump)
//            }
//        }
//
//        val referenceFile = referenceKlibDump.get().asFile
//        if (unsupported.isNotEmpty()) {
//            val referenceDump = if (referenceFile.exists() && referenceFile.isFile) {
//                tools.v2.loadKlibDump(referenceFile)
//            } else {
//                tools.v2.createKlibDump()
//            }
//
//            unsupported.map { unsupportedTarget ->
//                reportDiagnostic(
//                    KotlinToolingDiagnostics.AbiValidationUnsupportedTarget.invoke(unsupportedTarget.targetName)
//                )
//                mergedDump.inferAbiForUnsupportedTarget(referenceDump, unsupportedTarget)
//            }.forEach { inferredDump ->
//                mergedDump.merge(inferredDump)
//            }
//        }
//
//        mergedDump.print(abiDir.resolve(klibDumpName))
}*/
