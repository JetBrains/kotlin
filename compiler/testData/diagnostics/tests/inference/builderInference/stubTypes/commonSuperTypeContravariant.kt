// FIR_IDENTICAL
// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

