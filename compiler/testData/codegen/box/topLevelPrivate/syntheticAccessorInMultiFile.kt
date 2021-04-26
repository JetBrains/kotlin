// TARGET_BACKEND: JVM
// WITH_RUNTIME

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestKt")
package test

fun <T> eval(fn: () -> T) = fn()

private val prop = "O"

private fun test() = "K"

fun box(): String {
    return eval { prop + test() }
}
