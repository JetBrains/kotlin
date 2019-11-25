// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
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
