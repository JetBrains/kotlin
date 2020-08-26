/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

@DefaultImplementation(impl = TypeSpecificityComparator.NONE::class)
interface TypeSpecificityComparator : PlatformSpecificExtension<TypeSpecificityComparator> {
    fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean

    object NONE : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker) = false
    }
}
