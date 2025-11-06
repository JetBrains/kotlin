// MODULE: lib
// MODULE_KIND: LibraryBinary

// FILE: lib/Checks.java
package lib;

public class Checks {
    public static void check(int a, int b) {}
    public static void check(int a, Integer b) {}
    public static void check(Integer a, int b) {}
    public static void check(Integer a, Integer b) {}
    public static void check(Object a, Object b) {}
}


// MODULE: lib2
// MODULE_KIND: LibraryBinary

// FILE: org/jspecify/annotations/NullMarked.java
package org.jspecify.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.MODULE, ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullMarked {}
 
// FILE: lib/package-info.java
@NullMarked
package lib;

import org.jspecify.annotations.NullMarked;

// FILE: lib/Checks.java
package lib;

public class Checks {
    public static void check(int a, int b) {}
    public static void check(int a, Integer b) {}
    public static void check(Integer a, int b) {}
    public static void check(Integer a, Integer b) {}
    public static void check(Object a, Object b) {}
}


// MODULE: main(lib)
// FILE: main.kt
import lib.Checks

fun test(a: Int, b: Int) {
    Checks.check(a, b)
    Checks.check(5, b)
    Checks.check(a, 0)
    Checks.check("foo", 0)
}