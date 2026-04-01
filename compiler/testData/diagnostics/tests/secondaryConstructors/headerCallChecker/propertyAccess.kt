// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop) :
        this(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, integerLiteral, propertyDeclaration, secondaryConstructor,
thisExpression */
