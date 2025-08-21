// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: Wrap.java
class Wrap {
    public static void createWrap(final int type, final boolean wrapFirstElement) {}
    public static void createWrap(final String type, final boolean wrapFirstElement) {}
}

// FILE: test.kt
fun test() {
    Wrap.<!NONE_APPLICABLE!>createWrap<!>(1, <!DEBUG_INFO_MISSING_UNRESOLVED!>wrapFirstElement<!> = false)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
