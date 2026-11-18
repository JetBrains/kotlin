// LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB

// FILE: annotation.kt
package kotlin.internal

annotation class ImplicitIntegerCoercion

// FILE: main.kt
import kotlin.internal.ImplicitIntegerCoercion

fun foo(@ImplicitIntegerCoercion x: ULong) {}

fun test() {
    foo(<expr>2147483648</expr>)
}
