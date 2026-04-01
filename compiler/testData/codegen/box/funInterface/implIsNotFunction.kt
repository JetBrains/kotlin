// WITH_STDLIB

import kotlin.test.*

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f is Function<*>

fun box(): String {
    assertFalse(foo { "zzz" })
    return "OK"
}
