/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.name.StandardClassIds

/**
 * The `kotlin.internal.ReflectionPackageName` file annotation, which makes the compiler use the given
 * package name in the reflective information of the declarations of the annotated file, instead of the real one.
 *
 * [BatchingPackageInserter] adds this annotation to every file whose package it patches, so that the reflective
 * information (`KClass.qualifiedName`, `KClass.toString()`, the default `Any.toString()`, etc.) is not affected
 * by the package renaming, and thus is the same in grouping and non-grouping modes.
 *
 * Not every backend takes the annotation into account (see `IrFile.reflectionPackageName`), so this service acts as
 * an opt-in switch: test runners of the platforms supporting the annotation are expected to register it in
 * [TestServices] via `useAdditionalService { ReflectionPackageNameAnnotation }`, or, if they don't use [TestServices],
 * to pass it to `BatchingPackageInserter.PackageNamePatcher` directly.
 */
object ReflectionPackageNameAnnotation : TestService {
    val fqName: String = StandardClassIds.Annotations.ReflectionPackageName.asFqNameString()

    fun render(packageName: String): String = "$fqName(${packageName.quoteAsKotlinStringLiteral()})"

    private const val GENERATED_FILE_ANNOTATIONS_MARKER_PREFIX =
        "/* BatchingPackageInserter: generated file annotations; "
    private const val GENERATED_FILE_ANNOTATIONS_MARKER_SUFFIX = " */"
    private const val ORIGINAL_FILE_SUPPRESS_ANNOTATION_COMMENTED_MARKER =
        "original file-level @file:Suppress was commented"

    private val diagnosticMarkupRegex = Regex("""<![^>]*!>|<!>""")

    internal fun renderGeneratedFileAnnotationsMarker(
        hasOriginalFileSuppressAnnotation: Boolean,
        hasOriginalInvisibleReferenceFileSuppression: Boolean,
        originalFileSuppressAnnotationText: String? = null,
    ): String {
        val originalAnnotationDescription = when {
            originalFileSuppressAnnotationText != null ->
                "$ORIGINAL_FILE_SUPPRESS_ANNOTATION_COMMENTED_MARKER: ${originalFileSuppressAnnotationText.asCommentText()}"
            hasOriginalInvisibleReferenceFileSuppression ->
                "original file-level @file:Suppress containing \"INVISIBLE_REFERENCE\" was preserved"
            hasOriginalFileSuppressAnnotation ->
                "original file-level @file:Suppress was preserved"
            else ->
                "no original file-level @file:Suppress annotation existed"
        }
        return GENERATED_FILE_ANNOTATIONS_MARKER_PREFIX + originalAnnotationDescription + GENERATED_FILE_ANNOTATIONS_MARKER_SUFFIX
    }

    internal fun originalFileSuppressAnnotationWasCommented(markerAndAnnotations: String): Boolean =
        ORIGINAL_FILE_SUPPRESS_ANNOTATION_COMMENTED_MARKER in markerAndAnnotations

    internal fun originalFileSuppressAnnotationHadTrailingComma(markerAndAnnotations: String): Boolean {
        if (!originalFileSuppressAnnotationWasCommented(markerAndAnnotations)) return false
        val originalAnnotation = markerAndAnnotations
            .substringAfter("$ORIGINAL_FILE_SUPPRESS_ANNOTATION_COMMENTED_MARKER: ")
            .substringBefore(GENERATED_FILE_ANNOTATIONS_MARKER_SUFFIX)
        val argumentsStart = originalAnnotation.indexOf('(') + 1
        val argumentsEnd = originalAnnotation.lastIndexOf(')')
        return originalAnnotation.substring(argumentsStart, argumentsEnd).trimEnd().endsWith(',')
    }

    private val fileAnnotationPattern =
        """(?:<![^>]*!>|<!>)*@file:(?:<![^>]*!>|<!>)*${Regex.escape(fqName)}(?:<![^>]*!>|<!>)*\([^\r\n]*\)(?:<![^>]*!>|<!>)*"""
    private val generatedFileAnnotationsMarkerPattern =
        """${Regex.escape(GENERATED_FILE_ANNOTATIONS_MARKER_PREFIX)}[^\r\n]*${Regex.escape(GENERATED_FILE_ANNOTATIONS_MARKER_SUFFIX)}"""
    private const val GENERATED_FILE_SUPPRESSION_PATTERN =
        """(?<generatedSuppression>(?:<![^>]*!>|<!>)*@file:Suppress\((?s:.*?)\)(?:<![^>]*!>|<!>)*\r?\n)?"""

    /**
     * Matches the marker and the file-level annotations inserted by [BatchingPackageInserter]. Diagnostic markup may be
     * present when this regex is applied after metadata rendering.
     */
    internal val generatedFileAnnotationsRegex: Regex = Regex(
        "$generatedFileAnnotationsMarkerPattern\r?\n$GENERATED_FILE_SUPPRESSION_PATTERN$fileAnnotationPattern(?:\r?\n)?"
    )

    private fun String.asCommentText(): String = replace(diagnosticMarkupRegex, "")
        .replace(Regex("\\s+"), " ")
        .replace("/*", "/ *")
        .replace("*/", "* /")
        .trim()
}

val TestServices.reflectionPackageNameAnnotation: ReflectionPackageNameAnnotation? by TestServices.nullableTestServiceAccessor()
