// MODULE: lib

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

// FILE: lib/Lib.java
package lib;

public class Lib {
    public static void call(String text) {}
}


// MODULE: main(lib)
// FILE: main.kt
import lib.Lib

fun test(text: String?) {
    Lib.call(text)
}