// RUN_PIPELINE_TILL: FRONTEND

open class Base {
    val a: Any field: String = ""
}

class Child: Base() {
    fun foo() {
        val a: String = ::a.<!INITIALIZER_TYPE_MISMATCH!>get<!>()
        val b: String = this::a.<!INITIALIZER_TYPE_MISMATCH!>get<!>()
        val c: String = Child::a.<!INITIALIZER_TYPE_MISMATCH!>get<!>(this)
        val d: String = Base::a.<!INITIALIZER_TYPE_MISMATCH!>get<!>(this)
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, explicitBackingField, functionDeclaration, localProperty,
propertyDeclaration, stringLiteral, thisExpression */
