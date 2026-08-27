// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: javax/annotation/Nullable.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {}

// FILE: test/J.java
package test;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class J {
    @Nullable
    public String nullableField = null;

    @Nonnull
    public String notNullField = "";

    public String platformField = "";

    @Nullable
    public final String finalNullableField = null;

    @Nullable
    public static String staticNullableField = null;

    @Nullable
    public static final String CONSTANT = "";

    @Nullable
    public int primitiveField = 0;

    @Nullable
    public List<String> listField = null;
}

// FILE: box.kt
import test.J
import kotlin.reflect.*
import kotlin.test.*

fun check(expected: String, p: KProperty<*>) {
    assertEquals(expected, p.returnType.toString())
    assertEquals(expected, p.getter.returnType.toString())
    if (p is KMutableProperty<*>) {
        assertEquals(expected, p.setter.parameters.last().type.toString())
    }
}

val useK1 =
    Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

fun box(): String {
    check("kotlin.String?", J::nullableField)
    check("kotlin.String", J::notNullField)
    check("kotlin.String!", J::platformField)
    check("kotlin.String?", J::finalNullableField)

    check("kotlin.String?", J::staticNullableField)

    check("kotlin.String?", J::CONSTANT)
    if (useK1) {
        assertFalse(J::CONSTANT.isConst)
    } else {
        assertTrue(J::CONSTANT.isConst)
    }

    check("kotlin.Int", J::primitiveField)

    check("kotlin.collections.(Mutable)List<kotlin.String!>?", J::listField)

    return "OK"
}
