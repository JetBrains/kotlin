// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ReportReificationProblemsInDnnAndFlexible
// ISSUE: KTLC-399

// FILE: J.java

public class J {
    public static String str = "J";
}

// FILE: test.kt

// Reproduction of IntelliJ, Anki-Android cases
fun getArrayWhenIntersectionWithFlexibleType() = <!TYPE_INTERSECTION_AS_REIFIED_DEPRECATION_WARNING!>arrayOf<!>(1, J.str)

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaProperty, outProjection */
