// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-17570
// DIAGNOSTICS: -UNUSED_VARIABLE -SENSELESS_COMPARISON -UNUSED_PARAMETER

// KT-17570: Avoid creating FlexibleType instances if one of the bounds is error type

// FILE: J.java

public class J {
    public static <T> T getAny() {
        return null;
    }
}

// FILE: k.kt

fun test() {
    takeNotNull(J.getAny() ?: J())
}

fun takeNotNull(s: J) {}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, javaFunction, javaType, nullableType */
