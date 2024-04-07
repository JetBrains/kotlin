// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p2/foo.kt
package p2

class A {
    fun callA() {}
}

class B {
    fun callB() {}
}
// MODULE: lib2(lib)
// MODULE_KIND: LibraryBinary
// FILE: p3/bar.kt
package p3

import org.jetbrains.kotlin.fir.plugin.MyComposable
import p2.A

interface RowScope

inline fun Row(a: A, content: @MyComposable RowScope.() -> Unit) {
    a.callA()
}
// MODULE: main(lib, lib2)
// FILE: main.kt
import org.jetbrains.kotlin.fir.plugin.MyComposable
import p2.A
import p2.B
import p3.Row

fun AuthorAndReadTime(b: B) {
    Row(A()) {
        b.callB()
    }
}