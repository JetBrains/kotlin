// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {}

// FILE: test/A.kt
package test

abstract class A {
    abstract val annotated: String?
    abstract val unannotated: String?
}

// FILE: test/X.java
package test;

import javax.annotation.Nonnull;

public abstract class X extends A {
    @Override
    @Nonnull
    public String getAnnotated() { return ""; }

    @Override
    public String getUnannotated() { return null; }
}

// FILE: box.kt
import test.X
import kotlin.reflect.KProperty1
import kotlin.test.assertEquals

fun box(): String {
    // A Java getter overriding a Kotlin property: the nullability annotation on the getter takes precedence over the type of the
    // overridden Kotlin property.
    val annotated = X::class.members.single { it.name == "annotated" } as KProperty1<*, *>
    assertEquals("kotlin.String", annotated.returnType.toString())
    assertEquals("kotlin.String", annotated.getter.returnType.toString())

    val unannotated = X::class.members.single { it.name == "unannotated" } as KProperty1<*, *>
    assertEquals("kotlin.String?", unannotated.returnType.toString())

    return "OK"
}
