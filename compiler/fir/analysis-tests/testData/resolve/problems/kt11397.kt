// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-11397

// KT-11397: Improve diagnostic message for INACCESSIBLE_TYPE
// In K1, using a value whose type is inaccessible (e.g. private/protected nested class)
// would show a confusing "INACCESSIBLE_TYPE ... due to: <same type>" error.
// In K2, this diagnostic does not exist.

open class Container {
    protected class ProtectedType {
        fun value(): Int = 42
    }

    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>produce<!>(): ProtectedType = ProtectedType()
}

fun test() {
    val c = Container()
    val v = <!INFERRED_INVISIBLE_RETURN_TYPE_WARNING!>c.produce()<!>  // K1: INACCESSIBLE_TYPE; K2: no such diagnostic
    v.<!INVISIBLE_REFERENCE!>value<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, nestedClass,
propertyDeclaration */
