// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-29877
// WITH_STDLIB

// KT-29877: Null literals are trying to cast to not-null Nothing
fun foo() {
    if (<!SENSELESS_COMPARISON!>null != null<!>) {
        println(null<!UNSAFE_CALL!>.<!><!MISSING_DEPENDENCY_CLASS!>inv<!>())
    }
}

fun bar() {
    if (null is Int) {
        println(null<!UNSAFE_CALL!>.<!><!MISSING_DEPENDENCY_CLASS!>inv<!>())
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, isExpression */
