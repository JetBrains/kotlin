// FIR_IDENTICAL

package test

import test.Host.foo

object Host {
    inline fun <reified T> foo(): String {
        return "OK"
    }
}

fun test() = foo<Any>()
