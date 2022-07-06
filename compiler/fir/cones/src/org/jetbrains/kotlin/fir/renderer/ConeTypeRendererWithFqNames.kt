/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.name.ClassId

open class ConeTypeRendererWithFqNames() : ConeTypeRenderer() {

    constructor(builder: StringBuilder) : this() {
        this.builder = builder
    }

    override fun ClassId.render() {
        builder.append(asString())
    }
}