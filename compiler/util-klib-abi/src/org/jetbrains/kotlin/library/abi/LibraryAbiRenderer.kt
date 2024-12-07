/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.AbiRendererImpl

/** The default rendering implementation. */
@ExperimentalLibraryAbiReader
object LibraryAbiRenderer {
    /**
     * Render the [LibraryAbi] to the string representation.
     *
     * @param libraryAbi The [LibraryAbi] instance previously read by [LibraryAbiReader].
     * @param settings The rendering settings.
     */
    fun render(libraryAbi: LibraryAbi, settings: AbiRenderingSettings): String =
        buildString { render(libraryAbi, this, settings) }

    /**
     * Render the [LibraryAbi] to the string representation.
     *
     * @param libraryAbi The [LibraryAbi] instance previously read by [LibraryAbiReader].
     * @param output The output to write the rendered text to.
     * @param settings The rendering settings.
     */
    fun render(libraryAbi: LibraryAbi, output: Appendable, settings: AbiRenderingSettings): Unit =
        AbiRendererImpl(libraryAbi, settings, output).render()
}

/**
 * The settings applied during rendering of [LibraryAbi].
 *
 * @param renderedSignatureVersion The IR signature version to render. This should be a version among the versions
 *   listed in [LibraryAbi.signatureVersions].
 * @param renderManifest Whether KLIB manifest properties should be rendered.
 * @param renderDeclarations Whether declarations should be rendered.
 * @param indentationString The string used for indentation of nested declarations.
 * @param whenSignatureNotFound A handler that is executed when a signature is not found for a specific declaration.
 */
@ExperimentalLibraryAbiReader
class AbiRenderingSettings(
    val renderedSignatureVersion: AbiSignatureVersion,
    val renderManifest: Boolean = false,
    val renderDeclarations: Boolean = true,
    val indentationString: String = "    ",
    val whenSignatureNotFound: (AbiDeclaration, AbiSignatureVersion) -> String = { declaration, signatureVersion ->
        error("No signature $signatureVersion for ${declaration::class.java}, $declaration")
    }
)
