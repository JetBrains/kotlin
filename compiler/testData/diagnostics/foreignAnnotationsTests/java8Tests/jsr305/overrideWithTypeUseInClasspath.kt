// SOURCE_RETENTION_ANNOTATIONS
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
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

// FILE: spr/NotNull.java
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
@Nonnull
@TypeQualifierNickname
public @interface NotNull {
}


// FILE: J1.java

import spr.*;

public interface J1<T> {
    @Nullable
    T getFoo();
}

// FILE: J2.java

import spr.*;

public class J2 implements J1<String> {
    @NotNull
    public String getFoo() { return ""; }
}


// FILE: Nonnull.kt
package spr
import javax.annotation.meta.TypeQualifierNickname
import javax.annotation.Nonnull;

// This is a crucial part: NotNull defined in Java has no TYPE_USE target, so it has been applied only to the method
// While on use-site (main.kt) spr.NotNull shall be resolved to Kotlin version with TYPE among target
// So, even while our annotation is type use, we still should consider situation when it's applied to the method/parameter, but not to type
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.RUNTIME)
@Nonnull
@TypeQualifierNickname
annotation class NotNull

// FILE: main.kt

fun bar(j2: J2) {
    j2.foo.length // `j2.foo` is not nullable
}
