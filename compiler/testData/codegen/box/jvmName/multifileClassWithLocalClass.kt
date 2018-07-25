// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

@file:JvmName("Test")
@file:JvmMultifileClass
package test

fun foo(): String = bar()
fun bar(): String {
    class Local(val x: String)
    return Local("OK").x
}

fun box(): String = foo()
