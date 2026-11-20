// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.java
public class A {
    public String x = "A";
}

// FILE: B.java
public class B extends A {
    private String x = "B";
}

// FILE: C.java
public class C extends B {
}

// FILE: box.kt
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.test.assertEquals

fun box(): String {
    val x = B::class.members.single { it.name == "x" } as KProperty1<B, String>
    assertEquals(B::class.java, x.javaField!!.declaringClass)
    assertEquals(KVisibility.PRIVATE, x.visibility)
    x.isAccessible = true
    assertEquals("B", x.get(B()))

    assertEquals(emptyList(), C::class.members.filter { it.name == "x" })

    return "OK"
}
