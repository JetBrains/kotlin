// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// WARNING_FOR_JSR305_ANNOTATIONS

// FILE: MyNullable.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.MAYBE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyNullable {

}

// FILE: MyCheckForNull.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@CheckForNull
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCheckForNull {

}

// FILE: MyNonnull.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.ALWAYS)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyNonnull {

}

// FILE: A.java

import javax.annotation.*;

public class A {
    @MyNullable public String field = null;

    @MyNullable
    public String foo(@MyNonnull String x, @MyNullable CharSequence y) {
        return "";
    }

    @MyNonnull
    public String bar() {
        return "";
    }

    @MyCheckForNull
    public String baz(@MyNonnull String x, @MyCheckForNull CharSequence y) {
        return "";
    }
}

// FILE: main.kt

fun main(a: A) {
    a.foo("", null)?.length
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo("", null)<!>.length
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, "")<!>.length

    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.baz("", null)<!>.length
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.baz(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, "")<!>.length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.length
}
