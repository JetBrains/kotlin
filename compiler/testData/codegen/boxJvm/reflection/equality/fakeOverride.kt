// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertNotEquals

open class A<T> {
    fun foo(t: T) {}
}

open class B<U> : A<U>()

class C : B<String>()

fun box(): String {
    val afoo = A::class.members.single { it.name == "foo" }
    val bfoo = B::class.members.single { it.name == "foo" }
    val cfoo = C::class.members.single { it.name == "foo" }

    assertNotEquals(afoo, bfoo)
    assertNotEquals(afoo.hashCode(), bfoo.hashCode())
    assertNotEquals(bfoo, cfoo)
    assertNotEquals(bfoo.hashCode(), cfoo.hashCode())

    return "OK"
}
