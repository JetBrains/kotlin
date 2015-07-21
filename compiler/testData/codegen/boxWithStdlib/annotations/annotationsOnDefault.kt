import kotlin.test.assertEquals

annotation(retention = AnnotationRetention.RUNTIME) class Ann(val x: Int)
class A {
    Ann(1) fun foo(x: Int, y: Int = 2, z: Int) {}

    Ann(1) constructor(x: Int, y: Int = 2, z: Int)
}

class B @Ann(1) constructor(x: Int, y: Int = 2, z: Int) {}

fun test(name: String, annotations: Array<out Annotation>) {
    assertEquals(1, annotations.filterIsInstance<Ann>().single().x, "$name[0]")
}

fun box(): String {
    val foo = javaClass<A>().getDeclaredMethods().first { it.getName() == "foo" }
    test("foo", foo.getDeclaredAnnotations())

    val fooDefault = javaClass<A>().getDeclaredMethods().first { it.getName() == "foo\$default" }
    test("foo", foo.getDeclaredAnnotations())

    val (secondary, secondaryDefault) = javaClass<A>().getDeclaredConstructors().partition { it.getParameterTypes().size() == 3 }

    test("secondary", secondary[0].getDeclaredAnnotations())
    test("secondary\$default", secondaryDefault[0].getDeclaredAnnotations())

    val (primary, primaryDefault) = javaClass<B>().getConstructors().partition { it.getParameterTypes().size() == 3 }

    test("primary", primary[0].getDeclaredAnnotations())
    test("primary\$default", primaryDefault[0].getDeclaredAnnotations())

    return "OK"
}
