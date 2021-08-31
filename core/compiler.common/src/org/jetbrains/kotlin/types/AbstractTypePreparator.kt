/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.KotlinTypeMarker

abstract class AbstractTypePreparator {
    abstract fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker

    object Default : AbstractTypePreparator() {
        override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
            return type
        }
    }
}
