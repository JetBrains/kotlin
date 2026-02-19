// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun takeArray(array: Array<String>) {}

fun test() {
    "foo bar".<!UNRESOLVED_REFERENCE!>split<!>(<!UNSUPPORTED!>[""]<!>)
    <!UNRESOLVED_REFERENCE!>unresolved<!>(<!UNSUPPORTED!>[""]<!>)
    takeArray(<!UNSUPPORTED!>[""]<!>)
    val v = <!UNSUPPORTED!>[""]<!>
    <!UNSUPPORTED!>[""]<!>
    <!UNSUPPORTED!>[1, 2, 3]<!>.size
}

fun baz(arg: Array<Int> = <!UNSUPPORTED!>[]<!>) {
    if (true) <!UNSUPPORTED!>["yes"]<!> else {<!UNSUPPORTED!>["no"]<!>}
}

class Foo(
    val v: Array<Int> = <!UNSUPPORTED!>[]<!>
)

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, functionDeclaration, ifExpression, integerLiteral,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
