// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: androidx/annotation/RecentlyNullable.java
package androidx.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RecentlyNullable {}

// FILE: androidx/annotation/RecentlyNonNull.java
package androidx.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RecentlyNonNull {}

// FILE: test/J.java
package test;

import androidx.annotation.RecentlyNonNull;
import androidx.annotation.RecentlyNullable;

public class J {
    @RecentlyNullable
    public String nullableString() { return null; }

    @RecentlyNonNull
    public String notNullString() { return ""; }

    public void foo(@RecentlyNullable String nullable, @RecentlyNonNull String notNull) {}
}

// FILE: box.kt
import test.J
import kotlin.reflect.KType
import kotlin.test.assertEquals

fun check(expected: String, type: KType) {
    assertEquals(expected, type.toString())
}

fun box(): String {
    // Warning-level nullability annotations have no effect in kotlin-reflect.
    check("kotlin.String!", J::nullableString.returnType)
    check("kotlin.String!", J::notNullString.returnType)

    check("kotlin.String!", J::foo.parameters[1].type)
    check("kotlin.String!", J::foo.parameters[2].type)

    return "OK"
}
