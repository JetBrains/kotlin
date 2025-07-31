// RUN_PIPELINE_TILL: FRONTEND
open class A {
    open <!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> it: Number
        private field = 3
        <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>set(value)<!> {
            field = value.toInt()
        }

    fun test() {
        // error, because `it` is not
        // final, so no smart type narrowing
        // is provided
        println(it <!UNRESOLVED_REFERENCE!>+<!> 1)
    }
}

open class B : A() {
    override var it: Number
        get() = 3.14
        set(value) {}
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, functionDeclaration, getter, integerLiteral,
override, propertyDeclaration, setter */
