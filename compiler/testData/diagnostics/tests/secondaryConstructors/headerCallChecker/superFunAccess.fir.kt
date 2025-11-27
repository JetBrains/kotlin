// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// LATEST_LV_DIFFERENCE
open class B(x: Int) {
    fun foo() = 1
}
class A : B {
    constructor(x: Int, y: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.foo()) :
        super(<!ARGUMENT_TYPE_MISMATCH!>x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.foo()<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, primaryConstructor,
secondaryConstructor, thisExpression */
