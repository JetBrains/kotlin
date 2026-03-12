/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.AbiClassifierReference.ClassReference
import org.jetbrains.kotlin.types.Variance

/**
 * Shared utilities used by both [LibraryAbiReaderImpl] (IR-based) and [MetadataLibraryAbiReaderImpl] (descriptor-based).
 */

// region Visibility

@ExperimentalLibraryAbiReader
internal enum class VisibilityStatus(val isPubliclyVisible: Boolean) {
    PUBLIC(true), INTERNAL_PUBLISHED_API(true), NON_PUBLIC(false)
}

// endregion

// region Built-in type constants and helpers

@ExperimentalLibraryAbiReader
internal val KOTLIN_COMPOUND_NAME = AbiCompoundName("kotlin")

@ExperimentalLibraryAbiReader
internal val KOTLIN_ANY_QUALIFIED_NAME = AbiQualifiedName(KOTLIN_COMPOUND_NAME, AbiCompoundName("Any"))

@ExperimentalLibraryAbiReader
internal val KOTLIN_UNIT_QUALIFIED_NAME = AbiQualifiedName(KOTLIN_COMPOUND_NAME, AbiCompoundName("Unit"))

@ExperimentalLibraryAbiReader
internal fun isKotlinBuiltInType(type: AbiType, className: AbiQualifiedName, nullability: AbiTypeNullability): Boolean {
    if (type !is AbiType.Simple || type.nullability != nullability) return false
    return (type.classifierReference as? ClassReference)?.className == className
}

// endregion

// region Modality and Variance conversions

@ExperimentalLibraryAbiReader
internal fun Modality.toAbiModality(containingClassModality: AbiModality?): AbiModality = when (this) {
    Modality.FINAL -> AbiModality.FINAL
    Modality.OPEN -> if (containingClassModality == AbiModality.FINAL) AbiModality.FINAL else AbiModality.OPEN
    Modality.ABSTRACT -> AbiModality.ABSTRACT
    Modality.SEALED -> AbiModality.SEALED
}

@ExperimentalLibraryAbiReader
internal fun Variance.toAbiVariance(): AbiVariance = when (this) {
    Variance.INVARIANT -> AbiVariance.INVARIANT
    Variance.IN_VARIANCE -> AbiVariance.IN
    Variance.OUT_VARIANCE -> AbiVariance.OUT
}

@ExperimentalLibraryAbiReader
internal fun DescriptorVisibility.toVisibilityStatus(
    containingClassModality: AbiModality?,
    parentPropertyVisibilityStatus: VisibilityStatus? = null,
    hasPublishedApiAnnotation: Boolean = false,
): VisibilityStatus {
    if (parentPropertyVisibilityStatus != null && parentPropertyVisibilityStatus != VisibilityStatus.PUBLIC)
        return parentPropertyVisibilityStatus

    return when (this) {
        DescriptorVisibilities.PUBLIC -> VisibilityStatus.PUBLIC
        DescriptorVisibilities.PROTECTED -> {
            if (containingClassModality == AbiModality.FINAL)
                VisibilityStatus.NON_PUBLIC
            else
                VisibilityStatus.PUBLIC
        }
        DescriptorVisibilities.INTERNAL -> {
            if (hasPublishedApiAnnotation) VisibilityStatus.INTERNAL_PUBLISHED_API
            else VisibilityStatus.NON_PUBLIC
        }
        else -> VisibilityStatus.NON_PUBLIC
    }
}

/**
 * Converts a [ClassName][kotlin.metadata.ClassName] (e.g., "org/foo/Bar.Nested") to [AbiQualifiedName].
 * Package parts use `/` separators; nested class parts use `.` separators.
 */
@ExperimentalLibraryAbiReader
internal fun classNameToQualifiedName(className: String): AbiQualifiedName {
    // ClassName format: "org/foo/Bar" or "org/foo/Bar.Nested"
    // First, separate the package from the relative class name
    val dotIndex = className.indexOf('.')
    val topLevelPart = if (dotIndex >= 0) className.substring(0, dotIndex) else className

    val slashIndex = topLevelPart.lastIndexOf('/')
    val packageFqn = if (slashIndex >= 0) topLevelPart.substring(0, slashIndex).replace('/', '.') else ""

    // Relative name: "Bar" or "Bar.Nested"
    val classSimplePart = if (slashIndex >= 0) topLevelPart.substring(slashIndex + 1) else topLevelPart
    val relativeName = if (dotIndex >= 0) "$classSimplePart.${className.substring(dotIndex + 1)}" else classSimplePart

    return AbiQualifiedName(AbiCompoundName(packageFqn), AbiCompoundName(relativeName))
}

// endregion

// region Type parameter tag computation

private const val ALPHABET_SIZE: Int = 'Z' - 'A' + 1

/**
 * Computes the alphabetic tag prefix for a type parameter at the given [index].
 * Index 0 -> "A", 1 -> "B", ..., 25 -> "Z", 26 -> "AA", etc.
 */
internal fun computeTypeParameterTagPrefix(index: Int): String {
    val result = mutableListOf<Char>()

    var quotient = index
    var remainder = quotient % ALPHABET_SIZE
    do {
        result += ('A' + remainder)
        quotient /= ALPHABET_SIZE
        remainder = (quotient - 1) % ALPHABET_SIZE
    } while (quotient != 0)

    return if (result.size == 1) result[0].toString() else result.asReversed().joinToString(separator = "")
}

// endregion

// region Qualified name helpers

@ExperimentalLibraryAbiReader
internal fun qualifiedNameFromPackage(packageName: AbiCompoundName, topLevelSimpleName: String): AbiQualifiedName =
    AbiQualifiedName(packageName, AbiCompoundName(topLevelSimpleName))

@ExperimentalLibraryAbiReader
internal fun qualifiedNameFromParent(parentName: AbiQualifiedName, memberSimpleName: String): AbiQualifiedName =
    AbiQualifiedName(
        parentName.packageName,
        AbiCompoundName("${parentName.relativeName}${AbiCompoundName.SEPARATOR}$memberSimpleName")
    )

// endregion

// region Manifest reading

@ExperimentalLibraryAbiReader
internal fun KotlinLibrary.readAbiManifest(): LibraryManifest {
    val versions = versions
    return LibraryManifest(
        platform = builtInsPlatform?.name,
        platformTargets = buildList {
            nativeTargets.sorted().mapTo(this, LibraryTarget::Native)
            wasmTargets.sorted().mapTo(this, LibraryTarget::WASM)
        },
        compilerVersion = versions.compilerVersion,
        abiVersion = versions.abiVersion?.toString(),
        irProviderName = irProviderName
    )
}

// endregion
