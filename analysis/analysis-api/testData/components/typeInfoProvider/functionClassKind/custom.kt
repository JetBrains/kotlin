// WITH_FIR_TEST_COMPILER_PLUGIN

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.Scaffold

fun foo(block: @MyInlineable (Int) -> Unit) {}

fun test() {
    foo(x<caret>y)
}
