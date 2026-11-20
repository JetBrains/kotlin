// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/D1.java
package test;

public class D1 extends Base {
    public static String getX() {
        return "D1.getX";
    }

    public static String getY() {
        return "D1.getY";
    }
}

// FILE: test/J1.java
package test;

public class J1 extends D1 {
}

// FILE: test/D2.java
package test;

public class D2 {
    public static V getZ() {
        return null;
    }
}

// FILE: test/J2.java
package test;

public abstract class J2 extends D2 implements K {
}

// FILE: box.kt
package test

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticFunctions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Accessors of these properties have JVM signatures different from `getX()`/`getY()`/`getZ()`, so that Java classes can inherit
// both the property and the static method with the name of its getter.

open class Base {
    @JvmField
    val x: String = "Base.x"

    @get:JvmName("renamedGetY")
    val y: String = "Base.y"
}

@JvmInline
value class V(val value: String)

interface K {
    val z: V
}

private fun KClass<*>.checkMembers(vararg expected: String) {
    assertEquals(
        expected.toList(),
        members.filter { it.name != "equals" && it.name != "hashCode" && it.name != "toString" }.map { it.toString() }.sorted(),
    )
}

fun box(): String {
    J1::class.checkMembers(
        "fun getX(): kotlin.String!",
        "fun getY(): kotlin.String!",
        "val test.J1.x: kotlin.String",
        "val test.J1.y: kotlin.String",
    )
    assertEquals("D1.getX", J1::class.staticFunctions.single { it.name == "getX" }.call())
    assertEquals("D1.getY", J1::class.staticFunctions.single { it.name == "getY" }.call())
    val j1 = J1()
    assertEquals("Base.x", J1::class.memberProperties.single { it.name == "x" }.get(j1))
    assertEquals("Base.y", J1::class.memberProperties.single { it.name == "y" }.get(j1))

    J2::class.checkMembers(
        "fun getZ(): test.V!",
        "val test.J2.z: test.V",
    )
    assertEquals(listOf("getZ"), J2::class.staticFunctions.map { it.name })
    assertTrue(J2::class.memberProperties.single { it.name == "z" }.isAbstract)

    return "OK"
}
