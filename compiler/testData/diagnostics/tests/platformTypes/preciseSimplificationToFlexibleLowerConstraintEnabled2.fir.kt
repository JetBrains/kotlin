// LANGUAGE: +DontMakeExplicitJavaTypeArgumentsFlexible +PreciseSimplificationToFlexibleLowerConstraint
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78621

// FILE: JavaClass.java
public class JavaClass {
    public static <K> K simpleId(K k) { // fun <K> simpleId(k: K & Any..K?): K & Any..K? =
        return k;
    }
}

// FILE: main.kt

fun takeN(n: Number?): Int = 1

fun <T : CharSequence?> bar(n: T?, nn: T) {
    JavaClass.simpleId(n).length
    JavaClass.simpleId(nn).length
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, typeConstraint, typeParameter */
