// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/Outer.java
package test;

public class Outer<T> {
    public class Inner {
        public Inner self() { return this; }
        public Inner other(Inner x) { return x; }
    }
}

// FILE: box.kt
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals
import test.Outer

fun box(): String {
    val self = Outer.Inner::class.java.getMethod("self").kotlinFunction!!
    assertEquals("fun test.Outer<T>.Inner.self(): test.Outer<T!>.Inner!", self.toString())

    val other = Outer.Inner::class.java.getMethod("other", Outer.Inner::class.java).kotlinFunction!!
    assertEquals("fun test.Outer<T>.Inner.other(test.Outer<T!>.Inner!): test.Outer<T!>.Inner!", other.toString())

    return "OK"
}
