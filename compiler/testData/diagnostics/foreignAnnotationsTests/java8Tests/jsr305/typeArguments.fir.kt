// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// RENDER_PACKAGE: test
// SOURCE_RETENTION_ANNOTATIONS
// WITH_STDLIB
// JSR305_GLOBAL_REPORT: strict

// FILE: spr/Nullable.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
public @interface Nullable {
}

// FILE: spr/NonNullApi.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.TYPE_USE})
public @interface NonNullApi {
}

// FILE: test/package-info.java
@spr.NonNullApi()
package test;

// FILE: test/L.java
package test;

public class L<T extends java.util.Map<String, S>, S> {
    public T t() { return null; }
    public S s() { return null; }

    public void setT(@spr.Nullable T t) {}
    public void setS(S s) {}
}

// FILE: test/A.java
package test;

import spr.*;
import java.util.*;

public class A {
    public void foo(L<Map<String, Integer>, @Nullable Integer> l) {}
    public void bar(L<?, Integer> l) {}
    public L<Map<String, Integer>, @Nullable Integer> baz1() { return null; }
    public L<?, Integer> baz2() { return null; }
    public L<? extends Map<String, Integer>, Integer> baz3() { return null; }
}

// FILE: main.kt
import test.L

fun main(a: test.A, l: L<Map<String, Int>, Int?>, l1: L<Map<String, Int>, Int>) {
    a.foo(l)
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>l <!UNCHECKED_CAST!>as L<Map<String, Int>, Int><!><!>)
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>l <!UNCHECKED_CAST!>as L<Map<String, Int?>, Int?><!><!>)

    a.bar(l1)
    a.bar(<!ARGUMENT_TYPE_MISMATCH!>l1 <!UNCHECKED_CAST!>as L<Map<String, Int>, Int?><!><!>)

    a.baz1().t().containsKey("")
    a.baz1().t().<!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>containsKey<!>(null)
    a.baz1().t().containsValue(1)
    a.baz1().t().<!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>containsValue<!>(null)
    a.baz1().s().hashCode()

    a.baz1().setT(l.t())
    a.baz1().setT(<!ARGUMENT_TYPE_MISMATCH!>l.t() <!UNCHECKED_CAST!>as L<Map<String, Int>, Int><!><!>)
    a.baz1().setT(null)

    a.baz2().t().containsKey("")
    a.baz2().t().containsKey(null)
    a.baz2().t().containsValue(1)
    a.baz2().t().containsValue(null)
    a.baz2().s().hashCode()

    a.baz3().t().containsKey("")
    a.baz3().t().<!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>containsKey<!>(null)
    a.baz3().t().containsValue(1)
    a.baz3().t().<!TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>containsValue<!>(null)
    a.baz3().s().hashCode()
}
