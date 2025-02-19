// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

package test

import test.Derived.p

open class Base(val b: Boolean)

object Derived : Base(::p.isInitialized) {
    lateinit var p: String
}

fun box(): String {
    return if (Derived.b) "Fail" else "OK"
}
