// RUN_PIPELINE_TILL: FRONTEND

open class Base {
    open val foo: List<String> = listOf("1", "2")
    open val bar: Int = 0
}

class Derived : Base() {
    final override val foo: List<String>
        field = mutableListOf<String>("a", "b")

    final override val bar: Int
    <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!> = super.bar + 1

    fun usage() {
        foo[0] = "a"
        this.foo[0] = "a"
        super.foo<!NO_SET_METHOD!>[0]<!> = "a"
        bar.inc()
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, explicitBackingField, functionDeclaration,
integerLiteral, override, propertyDeclaration, smartcast, stringLiteral, superExpression, thisExpression */
