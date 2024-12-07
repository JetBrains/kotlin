// TARGET_BACKEND: JVM_IR

// FILE: dependency/Base.kt

package dependency;

enum class Base(val s: String) {
    FIRST("O"),
    SECOND("K")
}

// FILE: test/test.kt

package test

import dependency.Base.FIRST
import dependency.Base.SECOND

fun box(): String = FIRST.s + SECOND.s
