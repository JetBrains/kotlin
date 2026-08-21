// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J {
    private int y = 1;

    public int getY() {
        throw new AssertionError("J.y's getter should be called instead.");
    }

    public void setY(int value) {
        throw new AssertionError("J.y's setter should be called instead.");
    }
}

// FILE: box.kt

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

class A {
    private var x: Int = 1

    fun getX(): Int = error("A.x's getter should be called instead.")
    fun setX(value: Int): Unit = error("A.x's setter should be called instead.")
}

fun box(): String {
    val x = A::class.members.single { it.name == "x" } as KMutableProperty1<A, Int>
    x.isAccessible = true
    val a = A()
    assertEquals(Unit, x.set(a, 42))
    assertEquals(42, x.get(a))

    val y = J::class.members.single { it.name == "y" } as KMutableProperty1<J, Int>
    y.isAccessible = true
    val j = J()
    assertEquals(Unit, y.set(j, 42))
    assertEquals(42, y.get(j))

    return "OK"
}
