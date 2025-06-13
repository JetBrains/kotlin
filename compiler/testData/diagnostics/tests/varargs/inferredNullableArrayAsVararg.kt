// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: Test.java
public class Test {
    static public void foo(String ... x) {

    }
}

// FILE: test.kt
fun <T> select(vararg x: T) = x[1]

fun main(x: Array<String>?) {
    Test.foo(<!SPREAD_OF_NULLABLE!>*<!>(select(arrayOf(""), null))) // no compilation errors before the fix, NPE
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, javaFunction, nullableType, outProjection,
stringLiteral, typeParameter, vararg */
