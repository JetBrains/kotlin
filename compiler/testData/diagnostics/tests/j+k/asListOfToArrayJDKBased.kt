// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.Arrays

fun foo(x: ArrayList<String>, y: Array<String?>): List<String> {
    return Arrays.asList(*x.toArray(y))
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, nullableType */
