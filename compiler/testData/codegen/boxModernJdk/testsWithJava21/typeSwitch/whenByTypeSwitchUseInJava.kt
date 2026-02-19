// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

//FILE: kot/Repro.kt
package kot

import kot.JavaClass

fun ktest(b: Any) : String {
    return when (b) {
        is String -> "OK"
        is Int -> "Definitely Not OK"
        else -> "Not OK"
    }
}

fun box() = JavaClass().test("iamstring")


// FILE: kot/JavaClass.java
package kot;

import static kot.ReproKt.ktest;

public class JavaClass {
    public String test(Object o) {
        return ktest(o);
    }
}
