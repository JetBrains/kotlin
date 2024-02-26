// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p3/foo.kt
package p3

import org.jetbrains.kotlin.fir.plugin.MyComposable

fun setContent(content: @MyComposable () -> Unit): Int {
    content()
    return 3
}

// MODULE: main(lib)
// FILE: main.kt
import org.jetbrains.kotlin.fir.plugin.MyComposable
import p3.setContent

fun test(): Int {
    return setContent {
        Greeting("test")
    }
}

@MyComposable
fun Greeting(name: String) {
    show("hi $name!")
}

fun show(str: String) {}
