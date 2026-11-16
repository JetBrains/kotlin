// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 1
val A.prop: Int get() = 2

class A {
    constructor(x: Int)
    constructor() : this(
            <!ARGUMENT_TYPE_MISMATCH!><!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foobar<!>() +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foobar() <!NONE_APPLICABLE!>+<!>
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>.prop<!>
    )
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, propertyDeclaration, propertyWithExtensionReceiver, secondaryConstructor, thisExpression */
