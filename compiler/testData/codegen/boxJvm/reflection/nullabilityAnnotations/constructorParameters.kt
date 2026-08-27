// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: javax/annotation/Nullable.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {}

// FILE: test/J.java
package test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class J {
    public J(@Nullable String a) {}

    public class Inner {
        public Inner(@Nullable String b, @Nonnull String c) {}
    }
}

// FILE: test/JEnum.java
package test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum JEnum {
    ;

    JEnum(@Nullable String x, @Nonnull String y) {}
}

// FILE: box.kt
import test.J
import test.JEnum
import kotlin.reflect.KClass
import kotlin.test.assertEquals

private val KClass<*>.ctorParamTypes: String
    get() = constructors.single().parameters.joinToString(", ") { it.type.toString() }

fun box(): String {
    assertEquals("kotlin.String?", J::class.ctorParamTypes)

    assertEquals("test.J, kotlin.String?, kotlin.String", J.Inner::class.ctorParamTypes)
    assertEquals("kotlin.String?, kotlin.String", JEnum::class.ctorParamTypes)

    return "OK"
}
