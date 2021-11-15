// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt

package test

interface Z {
    fun a(): String
}

inline fun test(crossinline z: () -> String) =
    object : Z {
        val p = z()

        override fun a() = p
    }

// FILE: 2.kt

import test.*

fun box(): String {
    // This captured parameter would be added to object constructor
    val captured = "OK"
    var z: Any = "fail"
    val res = test {
        z = {
            captured
        }
        (z as Function0<String>)()
    }

    // Check that Java reflection doesn't crash. Actual values are tested in bytecodeListing/inline/enclosingInfo/.
    z.javaClass.enclosingConstructor
    z.javaClass.enclosingMethod
    z.javaClass.enclosingClass

    return res.a()
}
