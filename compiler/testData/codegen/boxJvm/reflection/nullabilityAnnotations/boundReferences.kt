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
    public void foo(@Nullable String nullable, @Nonnull String notNull, String platform) {}

    @Nullable
    public String nullableString() { return null; }

    @Nonnull
    public String notNullString(String platform, @Nullable String nullable) { return ""; }

    public class Inner {
        public Inner(@Nullable String nullable, @Nonnull String notNull, String platform) {}
    }
}

// FILE: box.kt
import test.A
import kotlin.reflect.KCallable
import kotlin.test.assertEquals

private val KCallable<*>.parameterTypes: String
    get() = parameters.joinToString(", ") { it.type.toString() }

fun box(): String {
    val a = A()

    assertEquals("test.A, kotlin.String?, kotlin.String, kotlin.String!", A::foo.parameterTypes)
    assertEquals("kotlin.String?, kotlin.String, kotlin.String!", a::foo.parameterTypes)

    assertEquals("kotlin.String?", a::nullableString.returnType.toString())

    assertEquals("kotlin.String", a::notNullString.returnType.toString())
    assertEquals("kotlin.String!, kotlin.String?", a::notNullString.parameterTypes)

    // Bound inner class constructor: the outer instance is the bound receiver.
    assertEquals("test.A, kotlin.String?, kotlin.String, kotlin.String!", A::Inner.parameterTypes)
    assertEquals("kotlin.String?, kotlin.String, kotlin.String!", a::Inner.parameterTypes)

    return "OK"
}
