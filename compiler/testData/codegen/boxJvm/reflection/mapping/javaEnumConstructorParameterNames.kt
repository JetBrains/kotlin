// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: ColorEnum.java
public enum ColorEnum {
    RED(255, 0, 0),
    GREEN(0, 255, 0),
    BLUE(0, 0, 255);

    private final int r, g, b;
    ColorEnum(int r, int g, int b) { this.r = r; this.g = g; this.b = b; }
    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }
}

// FILE: box.kt
// Tests that Java enum constructor parameters are reflected with their original
// field names, not generic positional names like arg0, arg1, arg2.

import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    val ctor = ColorEnum::class.constructors.single()
    val paramNames = ctor.parameters.map { it.name }

    assertEquals(3, paramNames.size,
        "ColorEnum constructor should have 3 parameters, got: $paramNames")

    // The parameter names must be the actual field names, not arg0/arg1/arg2
    for (name in paramNames) {
        assertFalse(name?.startsWith("arg") == true,
            "Expected field name, got positional placeholder: paramNames=$paramNames")
        assertNotNull(name,
            "Constructor parameter name must not be null: paramNames=$paramNames")
    }

    // Must be the field names in declaration order
    assertEquals(listOf("r", "g", "b"), paramNames,
        "Constructor parameter names should match field names r, g, b")

    return "OK"
}
