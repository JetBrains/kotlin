// TARGET_BACKEND: JVM
// FILE: ProtectedField.java

public abstract class ProtectedField {
    protected String field = "fail";
}

//FILE: test.kt
package test

import ProtectedField

class Derived: ProtectedField() {
    fun setAndGetField(arg: String) = myRun {
        super.field = arg
        super.field
    }
}

fun myRun(f: () -> String) = f()

fun box() = Derived().setAndGetField("OK")
