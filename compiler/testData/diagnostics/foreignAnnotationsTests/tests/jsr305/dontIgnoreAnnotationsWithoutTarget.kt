// FIR_IDENTICAL
// LANGUAGE: +TypeEnhancementImprovementsInStrictMode
// DIAGNOSTICS: -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict

// FILE: test/NonNullApi.java
package test;

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
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.PACKAGE})
public @interface NonNullApi {
}

// FILE: test/package-info.java
@NonNullApi
package test;

// FILE: test/A.java
package test;

import javax.annotation.Nullable;

public class A {
    @Nullable
    public String bar1() { return ""; }
}

// FILE: main.kt
import test.A

class Inv<T>(x: T)

fun foo(x: Inv<String?>) { }

fun main(a: A) {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String?>")!>Inv(a.bar1())<!>
    foo(x)
}
