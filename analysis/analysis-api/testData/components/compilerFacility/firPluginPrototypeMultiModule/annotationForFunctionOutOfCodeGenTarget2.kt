// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR
// CHECK_CALLS_WITH_ANNOTATION: org.jetbrains.kotlin.fir.plugin.MyComposable

// MODULE: main
// FILE: main.kt
import org.jetbrains.kotlin.fir.plugin.MyComposable
import p3.foo

@MyComposable
fun Greeting(): String {
    return "Hi $foo!"
}

// FILE: p3/foo.kt
package p3

import org.jetbrains.kotlin.fir.plugin.MyComposable

private var foo_ = 0

fun setFoo(newFoo: Int) {
    foo_ = newFoo
}

val foo: Int
    @MyComposable get() = foo_ + 1