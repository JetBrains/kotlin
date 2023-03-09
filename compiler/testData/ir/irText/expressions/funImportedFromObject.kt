// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57433

package test

import test.Host.foo

object Host {
    inline fun <reified T> foo(): String {
        return "OK"
    }
}

fun test() = foo<Any>()
