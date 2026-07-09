/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal class Usage(val base: Base) {
    fun decode(p: Boolean) {
        base.require(p) { "Message" }
    }
}
