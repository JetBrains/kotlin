// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

import java.util.Collections;
import java.util.List;

public class J implements KBase<String> {
    private static String storage = "";

    @Override
    public String getX() {
        return "x";
    }

    @Override
    public String getY() {
        return storage;
    }

    @Override
    public void setY(String y) {
        storage = y;
    }

    @Override
    public List<String> getZ() {
        return Collections.singletonList("z");
    }
}

// FILE: test/JGeneric.java
package test;

import java.util.List;

public class JGeneric<U> implements KBase<U> {
    @Override
    public U getX() {
        return null;
    }

    @Override
    public U getY() {
        return null;
    }

    @Override
    public void setY(U y) {
    }

    @Override
    public List<U> getZ() {
        return null;
    }
}

// FILE: box.kt
package test

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.declaredFunctions
import kotlin.test.*

interface KBase<T> {
    val x: T
    var y: T
    val z: List<T>
}

private fun check(klass: KClass<*>, vararg expected: String) {
    assertEquals(expected.toSet(), klass.memberProperties.map { it.toString() }.toSet())
    assertEquals(emptyList(), klass.declaredFunctions.map { it.name })

    for (property in klass.memberProperties) {
        assertTrue(property.isOpen, property.toString())
        assertFalse(property.isAbstract, property.toString())
        assertFalse(property.isFinal, property.toString())
    }
}

fun box(): String {
    check(
        J::class,
        "val test.J.x: kotlin.String!",
        "var test.J.y: kotlin.String!",
        "val test.J.z: kotlin.collections.List<kotlin.String!>",
    )
    check(
        JGeneric::class,
        "val test.JGeneric<U>.x: U!",
        "var test.JGeneric<U>.y: U!",
        "val test.JGeneric<U>.z: kotlin.collections.List<U!>",
    )

    val j = J()
    assertEquals("x", J::class.memberProperties.single { it.name == "x" }.get(j))
    assertEquals(listOf("z"), J::class.memberProperties.single { it.name == "z" }.get(j))

    val y = J::class.memberProperties.single { it.name == "y" } as KMutableProperty1<J, String>
    y.set(j, "OK")
    return y.get(j)
}
