/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * [Klib] represents an instance of Kotlin library (Klib) that can be read by the compiler or any other tools.
 *
 * In the future, this interface is supposed to replace [KotlinLibrary].
 *
 * The [Klib] consists of multiple components, each responsible for a certain aspect of the library.
 * There are the following "mandatory" components that are always present:
 * - [KlibMetadataComponent], which provides read access to the metadata stored inside the library.
 * - TODO(KT-81411): add more
 *
 * The component can be obtained by calling [getComponent]. For some components there exist
 * extension properties to ease the access and provide a good DSL-like experience. For example:
 * ```
 * val klib: Klib = ...
 * val metadata: KlibMetadataComponent = klib.metadata // Shortcut for `klib.getComponent(KlibMetadataComponent.ID)`
 * ```
 */
interface Klib {
    /**
     * The [KlibFile] that points to the library location on the file system.
     */
    val location: KlibFile

    /**
     * Get a specific [KlibMandatoryComponent] by its [kind]. Throw an error if the component is not found.
     */
    fun <KC : KlibMandatoryComponent> getComponent(kind: KlibMandatoryComponent.Kind<KC>): KC

    /**
     * Get a specific [KlibOptionalComponent] by its [kind]. Return `null` if the component is not found.
     */
    fun <KC : KlibOptionalComponent> getComponent(kind: KlibOptionalComponent.Kind<KC>): KC?
}

/**
 * A representation of a certain slice of the Klib library that can be read.
 */
sealed interface KlibComponent {
    /**
     * Kind (ID) of a [KlibComponent]. Used to access the component using [Klib.getComponent].
     */
    sealed interface Kind<KC : KlibComponent>
}

/**
 * A [KlibComponent] that is mandatory: This component is always present in the library.
 */
interface KlibMandatoryComponent : KlibComponent {
    interface Kind<KMC : KlibMandatoryComponent> : KlibComponent.Kind<KMC>
}

/**
 * A [KlibComponent] that is optional: This component is not available in the library if there is
 * no data that it can read according to [isDataAvailable].
 */
interface KlibOptionalComponent : KlibComponent {
    interface Kind<KOC : KlibOptionalComponent> : KlibComponent.Kind<KOC>

    /** Whether there is any data to be read by the component. */
    val isDataAvailable: Boolean
}

/**
 * The layout of a [KlibComponent]: Implementations of this abstract class provide access to the component's files by
 * the paths computed from the given [root].
 *
 * Important: The [root] is not necessarily the same as [Klib.location]. For example, a Klib could be a ZIP archive file.
 * In this case [root] points to the root of the virtual file system inside the archive, whereas [Klib.location] points
 * to the archive file itself. So, it is highly important to compute paths exactly based on [root].
 */
abstract class KlibComponentLayout(val root: KlibFile)
