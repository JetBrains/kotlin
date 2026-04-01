// RUN_PIPELINE_TILL: FRONTEND
// ISSUE #KT-40409

interface A {
    var b: B
}

interface B

fun A.test_1() {
    object : B by this.b {}
}

fun A.test_2() {
    object : B by b {}
}

class D(val x: String, val y: String = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.x) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
inheritanceDelegation, interfaceDeclaration, primaryConstructor, propertyDeclaration, thisExpression */
