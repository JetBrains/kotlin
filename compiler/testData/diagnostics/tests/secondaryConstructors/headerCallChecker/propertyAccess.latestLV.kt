// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> x <!NONE_APPLICABLE!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop) :
        this(<!ARGUMENT_TYPE_MISMATCH!>x <!NONE_APPLICABLE!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, integerLiteral, propertyDeclaration, secondaryConstructor,
thisExpression */
