// MODULE: lib
// FILE: p3/foo.kt
package p3;

fun foo() = 3
// MODULE: lib2(lib)
// FILE: p2/bar.kt
package p2;

fun bar() = 4 + p3.foo()
// MODULE: main(lib, lib2)
// MODULE_KIND: Source
// FILE: main.kt
import p2.bar

fun test() = bar()