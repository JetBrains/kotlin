// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB
// IGNORE_BACKEND: JS, JS_IR_ES6

package test

import test.Derived.p

open class Base(val b: Boolean)

object Derived : Base(::p.isInitialized) {
    lateinit var p: String
}

fun box(): String {
    return if (Derived.b) "Fail" else "OK"
}
