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

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class A {
    public A(@Nullable String s, @Nonnull String t, String u) {}

    public void foo(@Nullable String nullable, @Nonnull String notNull, String platform) {}

    @Nullable
    public String nullableString() { return null; }

    @Nonnull
    public String notNullString() { return ""; }

    @Nullable
    public static String staticMethod(@Nonnull String s) { return null; }

    public void primitive(@Nullable int x) {}

    @Nonnull
    public static int primitiveNotNull() { return 0; }

    @Nullable
    public List<String> nullableList() { return null; }

    public void varargNullable(@Nullable String... args) {}

    public void varargNotNull(@Nonnull String... args) {}

    public void varargPlatform(String... args) {}
}

// FILE: box.kt
import test.A
import kotlin.reflect.KType
import kotlin.test.assertEquals

fun check(expected: String, type: KType) {
    assertEquals(expected, type.toString())
}

fun box(): String {
    check("kotlin.String?", ::A.parameters[0].type)
    check("kotlin.String", ::A.parameters[1].type)
    check("kotlin.String!", ::A.parameters[2].type)

    check("kotlin.String?", A::foo.parameters[1].type)
    check("kotlin.String", A::foo.parameters[2].type)
    check("kotlin.String!", A::foo.parameters[3].type)

    check("kotlin.String?", A::nullableString.returnType)
    check("kotlin.String", A::notNullString.returnType)

    check("kotlin.String?", A::staticMethod.returnType)
    check("kotlin.String", A::staticMethod.parameters[0].type)

    check("kotlin.Int", A::primitive.parameters[1].type)
    check("kotlin.Int", A::primitiveNotNull.returnType)

    check("kotlin.collections.(Mutable)List<kotlin.String!>?", A::nullableList.returnType)

    assertEquals("kotlin.Array<(out) kotlin.String!>!", A::varargPlatform.parameters[1].type.toString())
    // `@Nullable` on a vararg parameter is ignored because a vararg parameter cannot have a nullable array type.
    assertEquals("kotlin.Array<(out) kotlin.String!>!", A::varargNullable.parameters[1].type.toString())
    assertEquals("kotlin.Array<(out) kotlin.String!>", A::varargNotNull.parameters[1].type.toString())

    return "OK"
}
