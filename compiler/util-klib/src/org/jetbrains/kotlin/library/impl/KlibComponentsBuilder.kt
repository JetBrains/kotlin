/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReaderFactory

// TODO (KT-81411): This class is an implementation detail. It should be made internal after dropping `KonanLibraryImpl`.
class KlibComponentsBuilder(private val layoutReaderFactory: KlibLayoutReaderFactory) {
    private val components: MutableMap<KlibComponent.Kind<*, *>, KlibComponent> = mutableMapOf()

    fun <KC : KlibComponent, KCL : KlibComponentLayout> withComponent(kind: KlibComponent.Kind<KC, KCL>): KlibComponentsBuilder {
        val layoutReader = layoutReaderFactory.createLayoutReader(kind::createLayout)
        kind.createComponentIfDataInKlibIsAvailable(layoutReader)?.let { component -> this.components[kind] = component }
        return this
    }

    fun build(): Map<KlibComponent.Kind<*, *>, KlibComponent> = components.toMap()
}
