// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION
import A.Inner

class A {
    inner class Inner
}

fun main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner */
