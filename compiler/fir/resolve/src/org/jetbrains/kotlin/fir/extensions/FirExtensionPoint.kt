/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.name.Name

abstract class FirExtensionPoint(val session: FirSession) {
    abstract val name: FirExtensionPointName

    fun interface Factory<P : FirExtensionPoint> {
        fun create(session: FirSession): P
    }
}

data class FirExtensionPointName(val name: Name) {
    constructor(name: String) : this(Name.identifier(name))
}
