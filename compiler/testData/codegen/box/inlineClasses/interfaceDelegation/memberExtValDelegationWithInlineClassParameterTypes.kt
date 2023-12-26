// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// JVM_ABI_K1_K2_DIFF: KT-63828

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String)

interface IFoo {
    val S.extVal: String
}

interface GFoo<T> {
    val T.extVal: String
}

object FooImpl : IFoo {
    override val S.extVal: String
        get() = x
}

object GFooImpl : GFoo<S> {
    override val S.extVal: String
        get() = x
}

class TestFoo : IFoo by FooImpl

class TestGFoo : GFoo<S> by GFooImpl

fun box(): String {
    with(TestFoo()) {
        assertEquals("OK", S("OK").extVal)
    }

    with(TestGFoo()) {
        assertEquals("OK", S("OK").extVal)
    }

    return "OK"
}
