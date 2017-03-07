// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestKt")
package test

private val prop = "O"

private fun test() = "K"

fun box(): String {
    return {
        prop + test()
    }()
}
