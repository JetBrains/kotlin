package foo

import kotlin.reflect.KClass

open class A

class B : A() {
    val a = 1
}

object O

interface I

enum class E {
    X,
    Y {
        val a = 1
    },
    Z {}
}

@JsName("Q")
class R

fun check(x: Any, y: Any, shouldBeEqual: Boolean = true, shouldBeSame: Boolean = true) {
    assertNotEquals(null, x)
    assertNotEquals(null, y)
    if (shouldBeEqual) {
        assertEquals(x, y)

        if (shouldBeSame && x !== y) {
            fail("Expected same instances, got expected = '$x', actual = '$y'")
        }
    }
    else {
        assertNotEquals(x, y)
    }
}

inline fun <reified T : Any> foo(b: Boolean = false): () -> KClass<T> {
    if (b) {
        val T = 1
    }
    return { T::class }
}

fun box(): String {
    check(A::class, foo<A>()())
    check(B::class, foo<B>()())
    check(O::class, foo<O>()())
    check(E::class, foo<E>()())

    return "OK"
}
