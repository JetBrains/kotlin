// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR
// CHECK_CALLS_WITH_ANNOTATION: org.jetbrains.kotlin.plugin.sandbox.MyInlineable

// MODULE: main
// FILE: main.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.foo

@MyInlineable
fun Greeting(): String {
    return "Hi $foo!"
}

// FILE: p3/foo.kt
package p3

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

private var foo_ = 0

fun setFoo(newFoo: Int) {
    foo_ = newFoo
}

val foo: Int
    @MyInlineable get() = foo_ + 1
