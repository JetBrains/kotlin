// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: jakarta/annotation/Nullable.java
package jakarta.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}

// FILE: edu/umd/cs/findbugs/annotations/CheckForNull.java
package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Note that in reality, it's CLASS, but here we're checking that all nullability annotations are recognized by FQ names.
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckForNull {}

// FILE: androidx/annotation/NonNull.java
package androidx.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Note that in reality, it's CLASS, but here we're checking that all nullability annotations are recognized by FQ names.
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNull {}

// FILE: org/checkerframework/checker/nullness/qual/Nullable.java
package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}

// FILE: test/J.java
package test;

public class J {
    @jakarta.annotation.Nullable
    public String jakartaNullable() { return null; }

    @edu.umd.cs.findbugs.annotations.CheckForNull
    public String findbugsCheckForNull() { return null; }

    @androidx.annotation.NonNull
    public String androidxNonNull() { return ""; }

    @org.checkerframework.checker.nullness.qual.Nullable
    public String checkerNullable() { return null; }
}

// FILE: box.kt
import test.J
import kotlin.reflect.KType
import kotlin.test.assertEquals

fun check(expected: String, type: KType) {
    assertEquals(expected, type.toString())
}

val useK1 =
    Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

fun box(): String {
    // Jakarta annotations are only supported since 2.2/2.4, see KTLC-285.
    check(if (useK1) "kotlin.String!" else "kotlin.String?", J::jakartaNullable.returnType)

    check("kotlin.String?", J::findbugsCheckForNull.returnType)
    check("kotlin.String", J::androidxNonNull.returnType)
    check("kotlin.String?", J::checkerNullable.returnType)

    return "OK"
}
