/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Represents the Kotlin file which provides additional, generated declarations for resolution.
 *
 * All member implementations should:
 * - consider caching the results for subsequent invocations.
 * - be lightweight and not build the whole file structure inside.
 * - not use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
 *
 * @see KaResolveExtension
 */
public abstract class KaResolveExtensionFile {
    /**
     * The name a Kotlin file which will be generated.
     *
     * Should have the `.kt` extension.
     *
     * It will be used as a Java facade name, e.g., for the file name `myFile.kt`, the `MyFileKt` facade is generated if the file contains some properties or functions.
     *
     * @see KaResolveExtensionFile
     */
    public abstract fun getFileName(): String

    /**
     * [FqName] of the package specified in the file
     *
     * The operation might be called regularly, so the [getFilePackageName] should work fast and avoid building the whole file text.
     *
     * It should be equal to the package name specified in the [buildFileText].
     *
     * @see KaResolveExtensionFile
     */
    public abstract fun getFilePackageName(): FqName

    /**
     * Returns the set of top-level classifier (classes, interfaces, objects, and type-aliases) names in the file.
     *
     * The result may have false-positive entries but cannot have false-negative entries. It should contain all the names in the package but may have some additional names that are not there.
     *
     * @see KaResolveExtensionFile
     */
    public abstract fun getTopLevelClassifierNames(): Set<Name>

    /**
     * Returns the set of top-level callable (functions and properties) names in the file.
     *
     * The result may have false-positive entries but cannot have false-negative entries. It should contain all the names in the package but may have some additional names that are not there.
     *
     * @see KaResolveExtensionFile
     */
    public abstract fun getTopLevelCallableNames(): Set<Name>

    /**
     * Creates the generated Kotlin source file text.
     *
     * The resulted String should be a valid Kotlin code.
     * It should be consistent with other declarations which are present in the [KaResolveExtensionFile], more specifically:
     * 1. [getFilePackageName] should be equal to the file's package name.
     * 2. All classifier names should be contained in the [getTopLevelClassifierNames].
     * 3. All callable names should be contained in the [getTopLevelCallableNames].
     *
     * Additional restrictions on the file text:
     * 1. The File should not contain the `kotlin.jvm.JvmMultifileClass` and `kotlin.jvm.JvmName` annotations on the file level.
     * 2. All declaration types should be specified explicitly.
     *
     * @see KaResolveExtensionFile
     */
    public abstract fun buildFileText(): String

    /**
     * Creates a [KaResolveExtensionNavigationTargetsProvider] for this [KaResolveExtensionFile].
     *
     * @see KaResolveExtensionNavigationTargetsProvider
     * @see KaResolveExtensionFile
     */
    public abstract fun createNavigationTargetsProvider(): KaResolveExtensionNavigationTargetsProvider
}

@Deprecated("Use 'KaResolveExtensionFile' instead", ReplaceWith("KaResolveExtensionFile"))
public typealias KtResolveExtensionFile = KaResolveExtensionFile