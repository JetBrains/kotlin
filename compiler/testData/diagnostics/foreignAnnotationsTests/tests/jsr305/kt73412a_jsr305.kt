// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict
// KT-73412, works with jspecify, see kt73412_jspecify.kt

// FILE: ann/Nullable.java
package ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
public @interface Nullable {
}

// FILE: ann/NonNullApi.java
package ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {
}

// FILE: api/package-info.java
@ann.NonNullApi
package api;

// FILE: api/Foo.java
package api;

public interface Foo<@ann.Nullable T> {
    void bar(T t);
}

// FILE: main.kt

import api.*

class FooImpl : Foo<String?> {
    override fun bar(s: String?) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl2<!> : Foo<String?> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(s: String) {}
}
