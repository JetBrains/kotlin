// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

class WithCompanion {
    companion object {
        var a = 0
        init {
            a++
        }
        @JvmField var b = a
        init {
            b++
        }
        val c = b
    }
}

object Object {
    var a = 0
    init {
        a++
    }
    @JvmField var b = a
    init {
        b++
    }
    val c = b
}

fun box(): String {
    assertEquals<Int>(2, WithCompanion.c, "Field WithCompanion.c")
    assertEquals<Int>(2, Object.c, "Field Object")
    return "OK"
}