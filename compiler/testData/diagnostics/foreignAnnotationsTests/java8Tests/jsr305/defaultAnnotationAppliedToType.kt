// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// SKIP_JAVAC
// SOURCE_RETENTION_ANNOTATIONS
// JSR305_GLOBAL_REPORT: strict

// FILE: spr/NonNullApi.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target({ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.TYPE_USE})
public @interface NonNullApi {
}

// FILE: spr/NullableApi.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull(when = When.MAYBE)
@TypeQualifierDefault({ElementType.TYPE_USE})
public @interface NullableApi {
}

// FILE: A.java
import spr.*;
import java.util.*;

@NonNullApi
public class A {
    public String foo(String x) { return ""; }
    public @NullableApi String bar(@NullableApi String y) { return ""; }
    public @NullableApi List<String> baz1() { return null; }
    public List<@NullableApi String> baz2() { return null; }
    public @NullableApi List<@NonNullApi String> baz3() { return null; }
}

// FILE: main.kt
fun main(a: A) {
    a.foo("").length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNNECESSARY_SAFE_CALL!>?.<!>length

    a.bar("")<!UNSAFE_CALL!>.<!>length
    a.bar(null)?.length

    a.baz1()<!UNSAFE_CALL!>.<!>get(0)<!UNSAFE_CALL!>.<!>length
    a.baz1()!!.get(0)<!UNSAFE_CALL!>.<!>length
    a.baz1()!!.get(0)?.length

    a.baz2().get(0)<!UNSAFE_CALL!>.<!>length
    a.baz2()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.get(0)<!UNSAFE_CALL!>.<!>length
    a.baz2()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.get(0)?.length

    a.baz3()<!UNSAFE_CALL!>.<!>get(0).length
    a.baz3()!!.get(0).length
    a.baz3()!!.get(0)<!UNNECESSARY_SAFE_CALL!>?.<!>length
}
