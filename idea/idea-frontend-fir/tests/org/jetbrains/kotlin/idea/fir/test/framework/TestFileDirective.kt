/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.test.framework

import java.util.*

abstract class TestFileDirective<VALUE : Any> {
    abstract val name: String
    abstract fun parse(value: String): VALUE?
}

class PresenceDirective(override val name: String) : TestFileDirective<Boolean>() {
    override fun parse(value: String): Boolean = true
}