// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: ignore

// FILE: MyErrorNonnull.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

import kotlin.annotations.jvm.*;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.ALWAYS)
@Retention(RetentionPolicy.RUNTIME)
@UnderMigration(status = MigrationStatus.STRICT)
public @interface MyErrorNonnull {
}

// FILE: MyWarnNonnull.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

import kotlin.annotations.jvm.*;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.ALWAYS)
@Retention(RetentionPolicy.RUNTIME)
@UnderMigration(status = MigrationStatus.WARN)
public @interface MyWarnNonnull {
}

// FILE: MyIgnoreNonnull.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

import kotlin.annotations.jvm.*;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.ALWAYS)
@Retention(RetentionPolicy.RUNTIME)
@UnderMigration(status = MigrationStatus.IGNORE)
public @interface MyIgnoreNonnull {
}

// FILE: A.java
public class A {
    public void foo(@MyErrorNonnull String bar) {}
    public void foo2(@MyWarnNonnull String bar) {}
    public void foo3(@MyIgnoreNonnull String bar) {}
    public void foo4(@MyMigrationNonnull String bar) {}
}

// FILE: main.kt
fun main(a: A) {
    a.foo("")
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    a.foo2("")
    a.foo2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    a.foo3("")
    a.foo3(null)

    a.foo4("")
    a.foo4(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
