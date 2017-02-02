// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: JavaClass.java

public class JavaClass {

    protected String FIELD = "OK";

}

// FILE: Kotlin.kt

package test

import JavaClass

class B : JavaClass() {
    inline fun bar() = FIELD
}

fun box() = B().bar()
