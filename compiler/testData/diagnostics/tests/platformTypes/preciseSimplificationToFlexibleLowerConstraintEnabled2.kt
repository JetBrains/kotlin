// LANGUAGE: +PreciseSimplificationToFlexibleLowerConstraint
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
    JavaClass.simpleId(n)<!UNSAFE_CALL!>.<!>length
    JavaClass.simpleId(nn)<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, typeConstraint, typeParameter */
