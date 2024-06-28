// MODULE: library
// KOTLINC_ARGS: -Xjvm-default=disable
// FILE: a.kt
package base

interface Check {
    fun test(): String {
        return "fail";
    }

    var test: String
        get() = "123"
        set(value) { value.length}
}

open class CheckClass : Check

// MODULE: main(library)
// KOTLINC_ARGS: -Xjvm-default=all
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
