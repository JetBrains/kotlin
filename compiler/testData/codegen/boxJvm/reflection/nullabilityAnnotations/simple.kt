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

// FILE: test/A.java
package test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class A {
    public A(@Nullable String s) {}

    public void foo(@Nullable String nullable, @Nonnull String notNull, String platform) {}

    @Nullable
    public String nullableString() { return null; }

    @Nonnull
    public String notNullString() { return ""; }
}

// FILE: box.kt
import test.A
import kotlin.reflect.KType
import kotlin.test.assertEquals

fun check(expectedIfEnhanced: String, type: KType) {
    // TODO (KT-88503): support nullability annotations in the new kotlin-reflect implementation.
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(expectedIfEnhanced, type.toString())
    } else {
        assertEquals("kotlin.String!", type.toString())
    }
}

fun box(): String {
    check("kotlin.String?", ::A.parameters.single().type)

    check("kotlin.String?", A::foo.parameters[1].type)
    check("kotlin.String", A::foo.parameters[2].type)
    assertEquals("kotlin.String!", A::foo.parameters[3].type.toString())

    check("kotlin.String?", A::nullableString.returnType)
    check("kotlin.String", A::notNullString.returnType)

    return "OK"
}
