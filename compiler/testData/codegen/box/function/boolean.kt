/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun bool_yes(): Boolean = true

fun box(): String {
    if (!bool_yes()) return "FAIL !bool_yes()"

    return "OK"
}
