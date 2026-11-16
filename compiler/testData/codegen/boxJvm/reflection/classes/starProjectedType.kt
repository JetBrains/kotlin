// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J<A, B> {
    public static class Nested<X> {}
    public class Inner<Y, Z> {}
}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.test.assertEquals
import kotlin.test.assertFalse

val STAR = KTypeProjection.STAR

class K<A, B> {
    class Nested<X>
    inner class Inner<Y, Z>
}

private fun check(outer: KClass<*>, nested: KClass<*>, inner: KClass<*>) {
    val o = outer.starProjectedType
    assertEquals(outer, o.classifier)
    assertEquals(listOf(STAR, STAR), o.arguments)
    assertEquals(o, outer.createType(listOf(STAR, STAR)))
    assertFalse(o.isMarkedNullable)

    val tp = outer.typeParameters.first()
    assertEquals(tp.createType(), tp.starProjectedType)
    assertFalse(tp.starProjectedType.isMarkedNullable)

    val n = nested.starProjectedType
    assertEquals(nested, n.classifier)
    assertEquals(listOf(STAR), n.arguments)
    assertFalse(n.isMarkedNullable)

    val i = inner.starProjectedType
    assertEquals(inner, i.classifier)
    assertEquals(listOf(STAR, STAR, STAR, STAR), i.arguments)
    assertFalse(i.isMarkedNullable)
}

fun box(): String {
    check(K::class, K.Nested::class, K.Inner::class)
    check(J::class, J.Nested::class, J.Inner::class)

    assertEquals(String::class, String::class.starProjectedType.classifier)
    assertEquals(listOf(), String::class.starProjectedType.arguments)

    var arr = Array<String>::class.starProjectedType
    assertEquals(Array<Any>::class, arr.classifier)
    assertEquals(listOf(STAR), arr.arguments)
    assertFalse(arr.isMarkedNullable)

    return "OK"
}
