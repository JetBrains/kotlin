// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FILE: J.java
public class J {
    public static J foo() { return null; }
}

// FILE: test.kt
fun test() {
    val [x] = <!COMPONENT_FUNCTION_MISSING!>J.foo()<!>
}

/* GENERATED_FIR_TAGS: destructuringDeclaration, flexibleType, functionDeclaration, javaFunction, localProperty,
propertyDeclaration */
