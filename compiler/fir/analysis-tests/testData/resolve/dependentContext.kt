// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DUMP_CFG: FLOW

class C(val x: Int)

fun foo() {
    val c = C(1)
    var b: Any?
    b = c
    if (b == c.also { b = "XXX" }) {
        b.<!UNRESOLVED_REFERENCE!>x<!>
    } else {
        b.<!UNRESOLVED_REFERENCE!>x<!>
    }
}

fun gau() {
    val q = true
    var r: Boolean?
    r = q
    if (q || r.also { r = null }<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {
        r<!UNSAFE_CALL!>.<!>accept()
    } else {
        r<!UNSAFE_CALL!>.<!>accept()
    }
}

fun Boolean.accept() {}

fun bar() {
    val e = C(1)
    var d: Any
    d = e
    (d.also { d = "XXXXXX" })<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    d.<!UNRESOLVED_REFERENCE!>x<!>
}

fun baz() {
    val g = C(1)
    var f: Any
    f = g
    f == g.also { f = "" }
    f.<!UNRESOLVED_REFERENCE!>x<!>
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
smartcast, stringLiteral */
