// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57433

package test

import test.Host.foo

object Host {
    inline fun <reified T> foo(): String {
        return "OK"
    }
}

fun test() = foo<Any>()
