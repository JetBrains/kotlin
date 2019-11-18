// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: A.kt

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("A")

package test

public val <T> Array<out T>.foo: String
    get() = this[0].toString() + this[1].toString()

// FILE: B.kt

import test.foo

fun box(): String = arrayOf('O', "K").foo
