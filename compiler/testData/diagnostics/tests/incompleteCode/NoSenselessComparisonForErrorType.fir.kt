// RUN_PIPELINE_TILL: FRONTEND
package a

fun foo() {
    val a = <!UNRESOLVED_REFERENCE!>getErrorType<!>()
    if (a == null) { //no senseless comparison

    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, propertyDeclaration */
