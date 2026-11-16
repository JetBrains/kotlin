// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B {
    val prop = 1
    constructor(x: Int, y: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x <!NONE_APPLICABLE!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop) :
        super(<!ARGUMENT_TYPE_MISMATCH!>x <!NONE_APPLICABLE!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
secondaryConstructor, thisExpression */
