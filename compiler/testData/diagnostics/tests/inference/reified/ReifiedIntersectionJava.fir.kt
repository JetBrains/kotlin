// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-52469
// LANGUAGE: +ProhibitIntersectionReifiedTypeParameter

// FILE: Platform.java

public class Platform {
    public static String flexibleString() {
        return "";
    }
}

// FILE: Main.kt

inline fun <reified T> select(vararg args: T) = args[0]

fun test() {
    <!TYPE_INTERSECTION_AS_REIFIED_ERROR!>select<!>(42, Platform.flexibleString())
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, inline, integerLiteral, nullableType, outProjection, reified,
typeParameter, vararg */
