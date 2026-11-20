// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/D.java
package test;

public abstract class D {
    public abstract String getX();

    public String getY() {
        return "D.y";
    }

    public String getZ() {
        return "D.z";
    }

    public void setZ(String z) {
    }

    public String getW() {
        return "D.w";
    }
}

// FILE: test/J1.java
package test;

public class J1 extends D implements K {
    @Override
    public String getX() {
        return "J1.x";
    }

    @Override
    public String getY() {
        return "J1.y";
    }
}

// FILE: test/J2.java
package test;

public abstract class J2 extends D implements K {
}

// FILE: test/J1b.java
package test;

public class J1b extends J1 implements K {
}

// FILE: test/J2b.java
package test;

public abstract class J2b extends J2 implements K {
}

// FILE: test/J2c.java
package test;

public abstract class J2c extends J2 implements KSub {
}

// FILE: box.kt
package test

import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface K {
    val x: String
    val y: String
    var z: String
}

interface KSub : K

class J3 : J2() {
    override val x: String get() = "J3.x"
}

private fun KClass<*>.checkMembers(vararg expected: String) {
    assertEquals(
        expected.toList(),
        members.filter { it.name != "equals" && it.name != "hashCode" && it.name != "toString" }.map { it.toString() }.sorted(),
    )
}

private fun KClass<*>.property(name: String): KProperty1<Any, *> =
    members.single { it.name == name } as KProperty1<Any, *>

fun box(): String {
    J1::class.checkMembers(
        "fun test.J1.getW(): kotlin.String!",
        "val test.J1.x: kotlin.String",
        "val test.J1.y: kotlin.String",
        "var test.J1.z: kotlin.String",
    )
    val j1 = J1()
    assertEquals("J1.x", J1::class.property("x").get(j1))
    assertEquals("J1.y", J1::class.property("y").get(j1))
    assertEquals("D.z", J1::class.property("z").get(j1))
    assertFalse(J1::class.property("x").isAbstract)
    assertFalse(J1::class.property("z").isAbstract)
    assertTrue(J1::class.property("z") is KMutableProperty1<*, *>)

    J2::class.checkMembers(
        "fun test.J2.getW(): kotlin.String!",
        "val test.J2.x: kotlin.String",
        "val test.J2.y: kotlin.String",
        "var test.J2.z: kotlin.String",
    )
    assertTrue(J2::class.property("x").isAbstract)
    assertFalse(J2::class.property("y").isAbstract)
    assertFalse(J2::class.property("z").isAbstract)

    for (klass in listOf(J2b::class, J2c::class)) {
        klass.checkMembers(
            "fun test.${klass.simpleName}.getW(): kotlin.String!",
            "val test.${klass.simpleName}.x: kotlin.String",
            "val test.${klass.simpleName}.y: kotlin.String",
            "var test.${klass.simpleName}.z: kotlin.String",
        )
        assertTrue(klass.property("x").isAbstract)
        assertFalse(klass.property("y").isAbstract)
        assertFalse(klass.property("z").isAbstract)
    }

    J1b::class.checkMembers(
        "fun test.J1b.getW(): kotlin.String!",
        "val test.J1b.x: kotlin.String",
        "val test.J1b.y: kotlin.String",
        "var test.J1b.z: kotlin.String",
    )
    assertFalse(J1b::class.property("z").isAbstract)

    J3::class.checkMembers(
        "fun test.J3.getW(): kotlin.String!",
        "val test.J3.x: kotlin.String",
        "val test.J3.y: kotlin.String",
        "var test.J3.z: kotlin.String",
    )
    val j3 = J3()
    assertEquals("J3.x", J3::class.property("x").get(j3))
    assertEquals("D.y", J3::class.property("y").get(j3))
    assertEquals("D.z", J3::class.property("z").get(j3))
    assertFalse(J3::class.property("x").isAbstract)
    assertFalse(J3::class.property("y").isAbstract)

    return "OK"
}
