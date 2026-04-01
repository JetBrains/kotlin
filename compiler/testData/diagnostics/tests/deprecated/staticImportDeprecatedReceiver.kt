// RUN_PIPELINE_TILL: BACKEND
// FILE: C.kt
package my

@Deprecated("")
class C {
    companion object {
        val a = ""
    }
}

@Deprecated("")
object O {
    val b = ""
}

// FILE: my/J.java
package my;

@Deprecated
public class J {
    public static int c = 1;
}

// FILE: test.kt
import my.<!DEPRECATION!>C<!>.Companion.a
import my.<!DEPRECATION!>O<!>.b
import my.<!DEPRECATION!>J<!>.c

fun test() {
    a
    b
    c
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, javaProperty, objectDeclaration,
propertyDeclaration, stringLiteral */
