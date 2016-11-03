// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val x: Int)
class A {
    @Ann(1) fun foo(x: Int, y: Int = 2, z: Int) {}

    @Ann(1) constructor(x: Int, y: Int = 2, z: Int)
}

class B @Ann(1) constructor(x: Int, y: Int = 2, z: Int) {}

fun test(name: String, annotations: Array<out Annotation>) {
    assertEquals(1, annotations.filterIsInstance<Ann>().single().x, "$name[0]")
}

fun box(): String {
    val foo = A::class.java.getDeclaredMethods().first { it.getName() == "foo" }
    test("foo", foo.getDeclaredAnnotations())

    val fooDefault = A::class.java.getDeclaredMethods().first { it.getName() == "foo\$default" }
    test("foo", foo.getDeclaredAnnotations())

    val (secondary, secondaryDefault) = A::class.java.getDeclaredConstructors().partition { it.getParameterTypes().size == 3 }

    test("secondary", secondary[0].getDeclaredAnnotations())
    test("secondary\$default", secondaryDefault[0].getDeclaredAnnotations())

    val (primary, primaryDefault) = B::class.java.getConstructors().partition { it.getParameterTypes().size == 3 }

    test("primary", primary[0].getDeclaredAnnotations())
    test("primary\$default", primaryDefault[0].getDeclaredAnnotations())

    return "OK"
}
