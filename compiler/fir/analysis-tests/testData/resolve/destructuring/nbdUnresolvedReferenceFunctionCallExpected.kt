// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm

class A

fun A.foo(): String = ""

fun test(a: A) {
    if (true) { (val <!UNRESOLVED_REFERENCE!>foo<!>) = a }
    if (true) { val (<!UNRESOLVED_REFERENCE!>foo<!>) = a }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, funWithExtensionReceiver, functionDeclaration,
localProperty, propertyDeclaration, stringLiteral */
