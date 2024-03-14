// FIR_IDENTICAL
// ISSUE: KT-61309
// ALLOW_KOTLIN_PACKAGE
// FILE: Test.java
package javacode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Test {
    Class<? extends Throwable> expected() default None.class;

    long timeout() default 0L;
}

// FILE: test.kt

package kotlin.test

typealias Test = javacode.Test

// FILE: main.kt

import kotlin.test.Test
import java.io.IOException

@Test(IOException::class)
fun someTest() {}

@Test(expected = IOException::class)
fun someRest() {}
