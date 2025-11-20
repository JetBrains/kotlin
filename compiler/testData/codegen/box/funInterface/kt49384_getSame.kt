// WITH_STDLIB

import kotlin.test.*

interface A<T>

// https://youtrack.jetbrains.com/issue/KT-49384
fun interface Foo<T> {
    fun same(obj: T): T
}

fun getSame(obj: A<out Any>, foo: Foo<A<out Any>>) = foo.same(obj)

fun box(): String {
    val obj = object : A<Any> {}
    assertSame(obj, getSame(obj) { it })

    return "OK"
}
