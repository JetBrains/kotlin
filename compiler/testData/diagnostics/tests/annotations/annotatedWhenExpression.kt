// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81915
fun foo() {}
fun main() {
    when (@<!UNRESOLVED_REFERENCE!>UnresolvedAnnotation<!> foo()) {
    }
    when (@<!UNRESOLVED_REFERENCE!>UnresolvedAnnotation<!> val x = foo()) {}
    when ((@<!UNRESOLVED_REFERENCE!>UnresolvedAnnotation<!> foo())) {}

    when (@Suppress("UNCHECKED_CAST") (Any() as List<String>)) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, whenExpression, whenWithSubject */
