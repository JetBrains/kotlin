// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-24507
// WITH_STDLIB

// KT-24507: SAM conversion accidentally applied to callable reference and incorrectly handled via BE

// FILE: A.java
public class A {
    public static void invokeLater(Runnable doRun) {
        doRun.run();
    }
}

// FILE: test.kt
fun foo(x: (() -> Unit) -> Unit) {
    x { println("Hi") }
}

fun main(args: Array<String>) {
    foo(A::<!INAPPLICABLE_CANDIDATE!>invokeLater<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
