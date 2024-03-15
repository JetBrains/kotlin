/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// Does not fail with TR.

public val z: Any = Z

private object Z

fun box(): String {
    if (z is Z)
        return "OK"
    else
        return "FAIL"
}
