// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/JBase.java
package test;

public class JBase {
    public int x;
}

// FILE: test/J.java
package test;

public class J extends JBase implements A {
    @Override
    public String getX() {
        return null;
    }
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

interface A {
    val x: String?
}

fun box(): String {
    assertEquals(
        listOf("val test.J.x: kotlin.String?", "var test.J.x: kotlin.Int"),
        J::class.members.filter { it.name == "x" }.map { it.toString() }.sorted(),
    )

    return "OK"
}
