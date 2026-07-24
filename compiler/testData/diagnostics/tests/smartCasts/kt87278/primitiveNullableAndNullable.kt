// RUN_PIPELINE_TILL: FRONTEND

fun String?.foo() = Unit

fun foo(a: String?, b: Any?) {
    if (a == b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
}

class Final

fun Final?.foo() = Unit

fun foo(a: Final?, b: Any?) {
    if (a === b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
    if (a == b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
}

enum class E { A; }

fun E?.foo() = Unit

fun foo(a: E?, b: Any?) {
    if (a == b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
}

open class NonPrimitive

fun NonPrimitive?.foo() = Unit

fun foo(a: NonPrimitive?, b: Any?) {
    if (a == b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
    if (a === b) {
        b.<!NONE_APPLICABLE!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
nullableType */
