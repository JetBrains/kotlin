// RUN_PIPELINE_TILL: FRONTEND

open class Base {
    open val foo: Any = "Base"
}

class Derived : Base() {
    final override val foo: Any
        field : Int = 1

    fun foo(): Int = <!RETURN_TYPE_MISMATCH!>(Base::foo).get(Derived())<!>
}

fun outside(): Int = <!RETURN_TYPE_MISMATCH!>(Base::foo).get(Derived())<!>

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, explicitBackingField, functionDeclaration, integerLiteral,
override, propertyDeclaration, stringLiteral */
