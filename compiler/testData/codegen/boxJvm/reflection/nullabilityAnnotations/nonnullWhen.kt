// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: javax/annotation/meta/When.java
package javax.annotation.meta;

public enum When {
    ALWAYS, UNKNOWN, MAYBE, NEVER
}

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.annotation.meta.When;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {
    When when() default When.ALWAYS;
}

// FILE: test/J.java
package test;

import javax.annotation.Nonnull;
import javax.annotation.meta.When;

public class J {
    @Nonnull(when = When.ALWAYS)
    public String always() { return ""; }

    @Nonnull
    public String defaultWhen() { return ""; }

    @Nonnull(when = When.MAYBE)
    public String maybe() { return null; }

    @Nonnull(when = When.NEVER)
    public String never() { return null; }

    @Nonnull(when = When.UNKNOWN)
    public String unknown() { return null; }

    public void foo(
        @Nonnull(when = When.ALWAYS) String always,
        @Nonnull(when = When.MAYBE) String maybe,
        @Nonnull(when = When.UNKNOWN) String unknown
    ) {}
}

// FILE: box.kt
import test.J
import kotlin.reflect.KType
import kotlin.test.assertEquals

fun check(expected: String, type: KType) {
    assertEquals(expected, type.toString())
}

fun box(): String {
    check("kotlin.String", J::always.returnType)
    check("kotlin.String", J::defaultWhen.returnType)
    check("kotlin.String?", J::maybe.returnType)
    check("kotlin.String?", J::never.returnType)
    check("kotlin.String!", J::unknown.returnType)

    check("kotlin.String", J::foo.parameters[1].type)
    check("kotlin.String?", J::foo.parameters[2].type)
    check("kotlin.String!", J::foo.parameters[3].type)

    return "OK"
}
