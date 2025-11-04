/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * [Klib] represents an instance of Kotlin library (Klib) that can be read by the compiler or any other tools.
 *
 * In the future, this interface is supposed to replace [KotlinLibrary].
 *
 * The [Klib] consists of multiple components, each responsible for a certain aspect of the library.
 * There are the following components that are supported out-of-the-box:
 * - [KlibMetadataComponent], which provides read access to the metadata stored inside the library.
 * - [KlibIrComponent], which provides read access to the IR stored inside the library.
 *
 * The component can be obtained by calling [getComponent]. For some components there exist
 * extension properties to ease the access and provide a good DSL-like experience. For example:
 * ```
 * val klib: Klib = ...
 * val metadata: KlibMetadataComponent = klib.metadata // A shortcut for `klib.getComponent(KlibMetadataComponent.ID)!!`
 * ```
 */
interface Klib {
    /**
     * The [KlibFile] that points to the library location on the file system.
     */
    val location: KlibFile

    /**
     * Get a specific [KlibComponent] by its [kind]. Return `null` if the component is not found.
     */
    fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>): KC?
}

/**
 * A representation of a certain slice of the Klib library that can be read.
 *
 * Note: The component is not available in the library if there is no data that it can read. The creation of the component and
 * the corresponding data presence check are performed in the [KlibComponent.Kind.createComponentIfDataInKlibIsAvailable].
 */
interface KlibComponent {
    /**
     * Kind (ID) of a [KlibComponent]. Used to access the component using [Klib.getComponent].
     */
    interface Kind<KC : KlibComponent, KCL : KlibComponentLayout> {
        /**
         * Create an instance [KlibComponentLayout] for the current component.
         */
        fun createLayout(root: KlibFile): KCL

        /**
         * Create an instance of the component.
         *
         * Note: If there is no data to be read by the component, no component instance is created and `null` is returned.
         */
        fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KCL>): KC?
    }
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
