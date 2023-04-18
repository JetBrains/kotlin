// FIR_IDENTICAL

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
