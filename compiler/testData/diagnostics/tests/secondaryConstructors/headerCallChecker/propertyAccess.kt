// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LATEST_LV_DIFFERENCE
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop) :
        this(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, integerLiteral, propertyDeclaration, secondaryConstructor,
thisExpression */
