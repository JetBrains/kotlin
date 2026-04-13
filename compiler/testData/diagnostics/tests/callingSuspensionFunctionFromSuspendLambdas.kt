// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68351
// FULL_JDK

import java.lang.ref.WeakReference

suspend fun test() {}

fun foo() {
    WeakReference<suspend () -> Unit> {
        test()
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, functionalType, javaFunction, lambdaLiteral, suspend */
