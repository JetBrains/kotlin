// KT-91552
// KT-91580
// MODULE: lib1
// MODULE_KIND: LibraryBinary
// FILE: lib1.kt
package lib

interface A
interface B
class AImpl : A
class BImpl : B

fun <T : A> foo(x: T): Int = 0

fun bar(vararg x: Int): Int = 0

// MODULE: lib2(lib1)
// MODULE_KIND: LibraryBinary
// FILE: lib2.kt
package lib

fun <T : B> foo(x: T): Int = 1

fun bar(x: IntArray): Int = 1

// MODULE: context(lib1, lib2)
// FILE: context.kt
import lib.*

fun test() {
    <caret_context>val x = 0
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
foo(AImpl()) + foo(BImpl()) + bar(1) + bar(intArrayOf(1))
