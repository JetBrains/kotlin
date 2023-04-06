// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57433

package test

import test.Foo.a
import test.Foo.foo

object Foo {
    val a: String = ""
    fun foo(): String = ""
}

val test1 = ::a
val test1a = Foo::a
val test2 = ::foo
val test2a = Foo::foo
