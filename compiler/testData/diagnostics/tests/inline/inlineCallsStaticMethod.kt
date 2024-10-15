// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: Test.java

public class Test {

    protected String data = "O";

    protected Test() {

    }

    protected static String testStatic() {
        return "K";
    }

}

// FILE: test.kt

public inline fun test(): String {
    val p = object : Test() {}
    return p.data + Test.<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>testStatic<!>();
}


fun box(): String {
    return test()
}
