// MODULE: dependency
// MODULE_KIND: LibraryBinary
// FILE: test/Test.java
package test;

public class Test {
    public void foo(Integer x) {}
}

// MODULE: main(dependency)
// FILE: org/jspecify/annotations/NullMarked.java
package org.jspecify.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.MODULE, ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullMarked {}

// FILE: module-info.java
import org.jspecify.annotations.NullMarked;

@NullMarked
module main {
    requires kotlin.stdlib;
}

// FILE: test/Test.java
package test;

public class Test {
    public void foo(Integer x) {}
}

// FILE: main.kt
import test.Test

fun main(x: Test) {
    x.foo(null)
}
