/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Represents a Kotlin file that contains generated declarations. It is provided by its [resolve extension][KaResolveExtension].
 *
 * The file content is built *lazily* with [buildFileText], which ensures that resolve extension files are only initialized when needed. The
 * additional information provided by functions like [getFilePackageName] and [getTopLevelClassifierNames] is used to avoid building the
 * resolve extension file's text unless absolutely necessary.
 *
 * All member implementations should:
 *
 * - Consider caching the results for subsequent invocations.
 * - Be lightweight and avoid building the whole file structure eagerly.
 * - Avoid using Kotlin resolution, as these functions are called during session initialization, so Analysis API access is forbidden.
 *
 * @see KaResolveExtension
 */
@KaExperimentalApi
public abstract class KaResolveExtensionFile {
    /**
     * The name of the Kotlin file which will be generated. It should have the `.kt` extension.
     *
     * If the file contains top-level properties or functions, the name will be used as a Java facade name. For example, given the file name
     * `myFile.kt`, a `MyFileKt` facade would be generated.
     */
    public abstract fun getFileName(): String

    /**
     * The [FqName] of the package that the resolve extension file belongs to.
     *
     * The function might be called regularly, so it should be fast and avoid building the whole file text.
     */
    public abstract fun getFilePackageName(): FqName

    /**
     * Returns the set of top-level classifier names (classes, interfaces, objects, and type-aliases) in the file. It must contain all such
     * names in the package, but may contain additional false positives.
     *
     * The function might be called regularly, so it should be fast and avoid building the whole file text.
     */
    public abstract fun getTopLevelClassifierNames(): Set<Name>

    /**
     * Returns the set of top-level callable names (functions and properties) in the file. It must contain all such names in the package,
     * but may contain additional false positives.
     *
     * The function might be called regularly, so it should be fast and avoid building the whole file text.
     */
    public abstract fun getTopLevelCallableNames(): Set<Name>

    /**
     * Builds the text of the generated Kotlin source file. It should be *valid* Kotlin code.
     *
     * The content *must* be consistent with other information provided by [KaResolveExtensionFile]:
     *
     * - The file's package name must be equal to [getFilePackageName].
     * - The names of all top-level classifiers declared in the file must be contained in [getTopLevelClassifierNames].
     * - The names of all top-level callables declared in the file must be contained in [getTopLevelCallableNames].
     *
     * In addition, the file text has the following restrictions:
     *
     * - The file should not contain the [JvmMultifileClass] and [JvmName] annotations on the file level.
     * - All declaration types should be specified explicitly.
     */
    public abstract fun buildFileText(): String

    /**
     * Creates a [KaResolveExtensionNavigationTargetsProvider] for this [KaResolveExtensionFile].
     */
    public abstract fun createNavigationTargetsProvider(): KaResolveExtensionNavigationTargetsProvider
}
