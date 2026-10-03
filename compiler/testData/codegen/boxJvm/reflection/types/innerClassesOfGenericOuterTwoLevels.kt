// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/JOuter.java
package test;

public class JOuter<T> {
    public class Mid<U> {
        public class Inner {
            public final String value;

            public Inner(T t, U u) { value = t + ":" + u; }

            public T t() { return null; }
            public JOuter<T>.Mid<U>.Inner self() { return this; }
        }
    }
}

// FILE: test/box.kt
package test

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

// Complements kt89280_innerClassOfGenericOuterInJava.kt and innerClassConstructor tests (KT-81854) with two levels of
// generic inner classes, where the constructor's instance parameter and return types mention type parameters of both outers.

class KOuter<T> {
    inner class Mid<U> {
        inner class Inner(val t: T, val u: U) {
            fun self(): Inner = this
        }
    }
}

private val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

private fun KFunction<*>.signature(): String =
    parameters.joinToString(prefix = "(", postfix = ") -> $returnType") { "${it.kind}: ${it.type}" }

fun box(): String {
    val kCtor = KOuter.Mid.Inner::class.constructors.single()
    assertEquals("(INSTANCE: test.KOuter<T>.Mid<U>, VALUE: T, VALUE: U) -> test.KOuter<T>.Mid<U>.Inner", kCtor.signature())
    assertEquals("fun test.KOuter<T>.Mid<U>.Inner.self(): test.KOuter<T>.Mid<U>.Inner", KOuter.Mid.Inner::class.members.single { it.name == "self" }.toString())
    val kInner = kCtor.call(KOuter<Int>().Mid<String>(), 1, "a")
    assertEquals("1:a", "${kInner.t}:${kInner.u}")

    // Star projections are created for type parameters of all outer classes (see KT-82093).
    assertEquals("test.KOuter<*>.Mid<*>.Inner", KOuter.Mid.Inner::class.starProjectedType.toString())

    val jInnerClass = JOuter.Mid.Inner::class
    assertEquals(
        "fun test.JOuter<T>.Mid<U>.Inner.self(): test.JOuter<T!>.Mid<U!>.Inner!",
        jInnerClass.members.single { it.name == "self" }.toString(),
    )
    assertEquals("fun test.JOuter<T>.Mid<U>.Inner.t(): T!", jInnerClass.members.single { it.name == "t" }.toString())

    val jCtor = jInnerClass.constructors.single()
    if (useK1) {
        // KT-85313: constructors of generic inner Java classes are loaded incorrectly in K1 reflection (`t` is lost).
        assertEquals("(INSTANCE: test.JOuter<T>.Mid<U>, VALUE: U!) -> test.JOuter<T>.Mid<U>.Inner", jCtor.signature())
    } else {
        assertEquals("(INSTANCE: test.JOuter<T>.Mid<U>, VALUE: T!, VALUE: U!) -> test.JOuter<T>.Mid<U>.Inner", jCtor.signature())
    }
    assertEquals("2:b", jCtor.call(JOuter<Int>().Mid<String>(), 2, "b").value)
    val bound = JOuter<Int>().Mid<String>()::Inner
    assertEquals("3:c", bound.call(3, "c").value)

    return "OK"
}
