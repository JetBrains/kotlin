// LANGUAGE: +MultiPlatformProjects +ImplicitSignedToUnsignedIntegerConversion
// TARGET_BACKEND: JVM_IR, NATIVE
// WITH_STDLIB

// MODULE: common
// FILE: annotation.kt

package kotlin.internal
annotation class ImplicitIntegerCoercion

// FILE: common.kt
import kotlin.internal.ImplicitIntegerCoercion

expect class Signed
expect value class Unsigned internal constructor(internal val data: Signed)

class FooUnsigned {
    constructor(@ImplicitIntegerCoercion x: Unsigned) {}
    constructor(@ImplicitIntegerCoercion y: String) {}
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual typealias Signed = Int
actual typealias Unsigned = UInt

fun box(): String {
    FooUnsigned(42)  // coercion
    FooUnsigned(42u)  // match
    FooUnsigned("42")  // match

    return "OK"
}
