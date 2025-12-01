/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReaderFactory

internal class KlibComponentsCache(private val layoutReaderFactory: KlibLayoutReaderFactory) {
    private val cache = mutableMapOf<KlibComponent.Kind<*, *>, Any>()

    fun <KC : KlibComponent, KCL : KlibComponentLayout> getComponent(kind: KlibComponent.Kind<KC, KCL>): KC? {
        val component = cache.getOrPut(kind) {
            val layoutReader = layoutReaderFactory.createLayoutReader(kind::createLayout)
            kind.createComponentIfDataInKlibIsAvailable(layoutReader).wrap()
        }.unwrap()

        @Suppress("UNCHECKED_CAST")
        return component as KC?
    }

    companion object {
        private val NULL_OBJECT = Any()

        private fun KlibComponent?.wrap(): Any = this ?: NULL_OBJECT
        private fun Any.unwrap(): KlibComponent? = if (this == NULL_OBJECT) null else this as KlibComponent
    }
}
