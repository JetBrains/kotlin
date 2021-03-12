// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

inline class S(val xs: Array<String>)

interface IFoo {
    var S.extVar: String
}

interface GFoo<T> {
    var T.extVar: String
}

object FooImpl : IFoo {
    override var S.extVar: String
        get() = xs[0]
        set(value) { xs[0] = value }
}

object GFooImpl : GFoo<S> {
    override var S.extVar: String
        get() = xs[0]
        set(value) { xs[0] = value }
}

class TestFoo : IFoo by FooImpl

class TestGFoo : GFoo<S> by GFooImpl

fun box(): String {
    with(TestFoo()) {
        val s = S(arrayOf("Fail 1"))
        s.extVar = "OK"
        assertEquals("OK", s.extVar)
    }

    with(TestGFoo()) {
        val s = S(arrayOf("Fail 2"))
        s.extVar = "OK"
        assertEquals("OK", s.extVar)
    }

    return "OK"
}
