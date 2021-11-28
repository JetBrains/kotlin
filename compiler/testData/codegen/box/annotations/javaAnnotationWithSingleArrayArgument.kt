// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: A.java

public class A {
    @Annos(value = @Anno(token = "OK"))
    @Strings(value = "OK")
    @Ints(value = 42)
    @Enums(value = E.EA)
    @Classes(value = double.class)
    public void test() {}
}

// FILE: box.kt

import kotlin.reflect.KClass
import kotlin.test.assertEquals

annotation class Anno(val token: String)
enum class E { EA }

annotation class Annos(val value: Array<Anno>)
annotation class Strings(val value: Array<String>)
annotation class Ints(val value: IntArray)
annotation class Enums(val value: Array<E>)
annotation class Classes(val value: Array<KClass<*>>)

class C : A()

fun box(): String {
    val annotations = C::class.java.getMethod("test").annotations.toList()
    assertEquals("OK", annotations.filterIsInstance<Annos>().single().value.single().token)
    assertEquals("OK", annotations.filterIsInstance<Strings>().single().value.single())
    assertEquals(42, annotations.filterIsInstance<Ints>().single().value.single())
    assertEquals(E.EA, annotations.filterIsInstance<Enums>().single().value.single())
    assertEquals(Double::class, annotations.filterIsInstance<Classes>().single().value.single())
    return "OK"
}
