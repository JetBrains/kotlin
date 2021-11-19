// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: V
// Caused by: java.lang.AssertionError: Unexpected IR element found during code generation. Either code generation for it is not implemented, or it should have been lowered:
// FUNCTION_REFERENCE 'public open fun getFoo (): @[FlexibleNullability] kotlin.String? declared in <root>.J' type=kotlin.reflect.KMutableProperty0<@[FlexibleNullability] kotlin.String?> origin=null reflectionTarget=<same>
// WITH_STDLIB

// MODULE: jjj
// FILE: J.java

public class J {
    private String s = "Fail";

    public void setFoo(String s) {
        this.s = s;
    }

    public String getFoo() {
        return s;
    }
}

// MODULE: lib(jjj)
// FILE: lib.kt
package k

import J

var p1 by J()::foo
var p2 by J()::foo

// MODULE: main(lib)
// FILE: box.kt

import k.p1
import k.p2

fun box(): String {
    p1 = "O"
    p2 = "K"
    return p1 + p2
}

