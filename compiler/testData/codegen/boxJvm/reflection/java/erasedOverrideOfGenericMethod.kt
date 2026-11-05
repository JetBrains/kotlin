// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JavaI.java
public interface JavaI {
    <T> CharSequence f(T x);
}

// FILE: J.java
public abstract class J implements KotlinI, JavaI {
    public String f(Object x) { return "J"; }

    public String g(java.util.List x) { return "J"; }
}

// FILE: box.kt
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals

interface KotlinI {
    fun <T> f(x: T): CharSequence
    fun <T> g(x: List<T>): CharSequence
}

abstract class K : KotlinI, JavaI {
    fun f(x: Any?): String = "K"
    fun g(x: List<Any?>): String = "K"
}

fun box(): String {
    for (name in listOf("f", "g")) {
        val f = J::class.members.single { it.name == name } as KFunction<*>
        assertEquals(J::class.java.declaredMethods.single { it.name == name && !it.isBridge }, f.javaMethod)
    }

    for (name in listOf("f", "g")) {
        assertEquals(2, K::class.members.filter { it.name == name }.size, "Fail: K.$name")
    }
    return "OK"
}
