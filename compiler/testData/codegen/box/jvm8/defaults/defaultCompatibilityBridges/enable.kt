// TARGET_BACKEND: JVM
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface Check {
    fun test(): String {
        return "fail";
    }

    var test: String
        get() = "fail"
        set(value) { value.length}
}

open class CheckClass : Check

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: source.kt
import base.*

interface SubCheck : Check {
    override fun test(): String {
        return "OK"
    }

    override var test: String
        get() = "OK"
        set(value) {
            value.length
        }
}

class SubCheckClass : CheckClass(), SubCheck

fun box(): String {
    val c = SubCheckClass()
    if (c.test() != "OK") return "Fail: " + c.test()

    c.test = ""
    return c.test
}
