// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

import java.util.List;

public class J {
    public static String string() { return ""; }
    public static List<Object> list() { return null; }
    public static int primitiveInt() { return 0; }
    public static Integer wrapperInt() { return 0; }
    public static <T> T generic(T t) { return t; }
    public static List raw() { return null; }
}

// FILE: K.kt
package test

import kotlin.reflect.*
import kotlin.test.assertEquals

object K {
    fun string() = J.string()
    fun list() = J.list()
    fun primitiveInt() = J.primitiveInt()
    fun wrapperInt() = J.wrapperInt()
    fun raw() = J.raw()
}

fun check(callable: KCallable<*>, expectedClassifier: KClassifier, expectedToString: String, checkJavaClass: ((Class<*>) -> Unit)? = null) {
    val type = callable.returnType
    assertEquals(expectedToString, type.toString())
    assertEquals(expectedClassifier, type.classifier)
    if (checkJavaClass != null) {
        checkJavaClass((type.classifier as KClass<*>).java)
    }
}

fun box(): String {
    check(J::string, String::class, "kotlin.String!")
    check(K::string, String::class, "kotlin.String!")

    check(J::list, List::class, "kotlin.collections.(Mutable)List<kotlin.Any!>!")
    check(K::list, List::class, "kotlin.collections.(Mutable)List<kotlin.Any!>!")

    check(J::primitiveInt, Int::class, "kotlin.Int") {
        assertEquals(Int::class.javaPrimitiveType!!, it)
    }
    check(K::primitiveInt, Int::class, "kotlin.Int") {
        assertEquals(Int::class.javaPrimitiveType!!, it)
    }

    check(J::wrapperInt, Int::class, "kotlin.Int!") {
        assertEquals(Int::class.javaObjectType, it)
    }
    check(K::wrapperInt, Int::class, "kotlin.Int!") {
        assertEquals(Int::class.javaObjectType, it)
    }

    check(J::raw, List::class, "kotlin.collections.MutableList<(raw) kotlin.Any?>")
    check(K::raw, List::class, "kotlin.collections.MutableList<(raw) kotlin.Any?>")

    val ref: (Number) -> Number = J::generic
    val generic = ref as KCallable<*>
    check(generic, generic.typeParameters.single(), "T!")

    return "OK"
}
