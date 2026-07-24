// RUN_PIPELINE_TILL: FRONTEND

// FILE: q/Utils.java
package q;

class Utils {
    static String fooString() { return "!"; }
    static Final fooFinal() { return new Final(); }
    static E fooE() { return E.A; }
    static NonPrimitive fooNonPrimitive() { return new NonPrimitive(); }
    static Object fooAny() { return new Object(); }
}

// FILE: q/main.kt
package q

fun <T> id(t: T): T = t

fun String.foo() = Unit

fun fooString() {
    val a = Utils.fooString()
    val b = Utils.fooAny()
    if (a == b) {
        b.foo()
    }
}

class Final

fun Final.foo() = Unit

fun fooFinal() {
    val a = Utils.fooFinal()
    val b = Utils.fooAny()
    if (a == b) {
        <!DEBUG_INFO_EXPRESSION_TYPE("(q.Final..q.Final?)")!>b<!>.foo()
    }
    if (b === a) {
        b.foo()
        var x = id(b)
        x = null
    }
}

enum class E { A; }

fun E.foo() = Unit

fun fooE() {
    val a = Utils.fooE()
    val b = Utils.fooAny()
    if (a == b) {
        b.foo()
    }
}

open class NonPrimitive

fun NonPrimitive.foo() = Unit

fun fooNonPrimitive() {
    val a = Utils.fooNonPrimitive()
    val b = Utils.fooAny()
    if (a == b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
    if (b === a) {
        b.foo()
        var x = id(b)
        x = null
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
nullableType */
