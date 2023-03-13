// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
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

@Target({ElementType.METHOD, ElementType.PARAMETER})
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

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.TYPE_USE})
public @interface NonNullApi {
}

// FILE: A.java
import spr.*;

@NonNullApi
public class A {
    public String field = null;

    public String foo(String x, @Nullable CharSequence y) {
        return "";
    }

    public String bar() {
        return "";
    }

    @Nullable
    public java.util.List<String> baz() {
        return null;
    }
}

// FILE: main.kt
fun main(a: A) {
    a.foo("", null)<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.foo("", null).length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.field.length

    a.baz()<!UNSAFE_CALL!>.<!>get(0)
    a.baz()!!.get(0).get(0)
    a.baz()!!.get(0)<!UNNECESSARY_SAFE_CALL!>?.<!>get(0)
}
