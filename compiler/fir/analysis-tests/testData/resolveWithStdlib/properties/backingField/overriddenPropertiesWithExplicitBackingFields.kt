// RUN_PIPELINE_TILL: FRONTEND
open class A {
    open <!VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>var<!> it: Number
        <!NON_FINAL_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!><!WRONG_MODIFIER_TARGET!>private<!> field = 3<!>
        set(value) {
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

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, explicitBackingField, functionDeclaration,
getter, integerLiteral, override, propertyDeclaration, setter */
