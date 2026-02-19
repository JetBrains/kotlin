// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// DUMP_INFERENCE_LOGS: MARKDOWN


// FILE: J.java
public class J {
    public static <F> java.util.List<F> asList(F[] a) {return null;}
}

// FILE: main.kt

fun foo(x: ArrayList<String>, y: Array<String?>): List<String> {
    return J.asList(x.toArray(y))
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, nullableType */
