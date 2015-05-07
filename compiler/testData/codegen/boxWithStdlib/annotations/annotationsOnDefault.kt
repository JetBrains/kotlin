import java.lang.annotation.*
import kotlin.reflect.jvm.java
import kotlin.test.assertEquals

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(val x: Int)
class A {
    Ann(1) fun foo(Ann(2) x: Int, y: Int = 2, Ann(3) z: Int) {}

    Ann(1) constructor(Ann(2) x: Int, y: Int = 2, Ann(3) z: Int)
}

class B [Ann(1)] (Ann(2) x: Int, y: Int = 2, Ann(3) z: Int) {}

fun Array<out Annotation>.ann() = filterIsInstance<Ann>()

fun test(name: String, annotations: Array<out Annotation>, parameters: Array<out Array<out Annotation>>) {
    assertEquals(1, annotations.ann()[0].x, "$name[0]")

    assertEquals(2, parameters[0].ann()[0].x, "$name-param[0]")
    assertEquals(0, parameters[1].ann().size(), "$name-param[1]")
    assertEquals(3, parameters[2].ann()[0].x, "$name-param[2]")
}

fun box(): String {
    val foo = javaClass<A>().getDeclaredMethods().first { it.getName() == "foo" }
    test("foo", foo.getDeclaredAnnotations(), foo.getParameterAnnotations())

    val fooDefault = javaClass<A>().getDeclaredMethods().first { it.getName() == "foo\$default" }
    test("foo", foo.getDeclaredAnnotations(), foo.getParameterAnnotations())

    val (secondary, secondaryDefault) = javaClass<A>().getDeclaredConstructors().partition { it.getParameterTypes().size() == 3 }

    test("secondary", secondary[0].getDeclaredAnnotations(), secondary[0].getParameterAnnotations())
    test("secondary\$default", secondaryDefault[0].getDeclaredAnnotations(), secondaryDefault[0].getParameterAnnotations())

    val (primary, primaryDefault) = javaClass<B>().getConstructors().partition { it.getParameterTypes().size() == 3 }

    test("primary", primary[0].getDeclaredAnnotations(), primary[0].getParameterAnnotations())
    test("secondary\$default", primaryDefault[0].getDeclaredAnnotations(), primaryDefault[0].getParameterAnnotations())

    return "OK"
}
