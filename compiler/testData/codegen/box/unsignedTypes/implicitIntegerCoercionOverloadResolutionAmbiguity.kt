// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE

// IGNORE_BACKEND_K1: ANDROID
// STATUS:
//  This line has been added because of the `AndroidRunner` test.
//  This test runner appends a path-like prefix to all fully-qualified names,
//  but the compiler logic for `ImplicitSignedToUnsignedIntegerConversion`
//  relies on the ability to check exactly `kotlin.internal.ImplicitIntegerCoercion`.

// ISSUE: KT-57484
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// WITH_STDLIB

// FILE: annotation.kt

package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: test.kt

import kotlin.internal.ImplicitIntegerCoercion

class FooInt {
    constructor(@ImplicitIntegerCoercion x: Int) {}
    constructor(@ImplicitIntegerCoercion y: String) {}
}

class FooUInt {
    constructor(@ImplicitIntegerCoercion x: UInt) {}
    constructor(@ImplicitIntegerCoercion y: String) {}
}

typealias myUInt = UInt
class FooMyUInt {
    constructor(@ImplicitIntegerCoercion x: myUInt) {}
    constructor(@ImplicitIntegerCoercion y: String) {}
}

fun box(): String {
    FooInt(19)  // overload match
    // `FooInt(19u)` is invalid in K1/N and K2/N
    FooInt("19")  // overload match

    FooUInt(42)  // coercion
    FooUInt(42u)  // overload match
    FooUInt("42")  // overload match

    FooMyUInt(153)  // coercion
    FooMyUInt(153u)  // overload match
    FooMyUInt("153")  // overload match

    return "OK"
}