// RUN_PIPELINE_TILL: FRONTEND

open class Base {
    val a: Any field: String = ""
}

class Child: Base() {
    fun foo() {
        val a: String <!INITIALIZER_TYPE_MISMATCH!>=<!> ::a.get()
        val b: String <!INITIALIZER_TYPE_MISMATCH!>=<!> this::a.get()
        val c: String <!INITIALIZER_TYPE_MISMATCH!>=<!> Child::a.get(this)
        val d: String <!INITIALIZER_TYPE_MISMATCH!>=<!> Base::a.get(this)
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, explicitBackingField, functionDeclaration, localProperty,
propertyDeclaration, stringLiteral, thisExpression */
