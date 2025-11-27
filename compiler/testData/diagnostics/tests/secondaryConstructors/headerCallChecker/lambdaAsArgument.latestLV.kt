// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// LATEST_LV_DIFFERENCE

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: () -> Int)
    constructor() : this(
            {
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>.foo() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foobar<!>()
            })
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, secondaryConstructor, thisExpression */
