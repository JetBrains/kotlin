// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ImprovedResolutionInSecondaryConstructors
// ISSUE: KT-77275
abstract class Super(x: String)

val y: String = ""

class A(x: String = y) : Super(y) {
    constructor(
        w: Int,
        z: Int,
        x: String <!INITIALIZER_TYPE_MISMATCH!>=<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>y<!>,
    ) : this(<!ARGUMENT_TYPE_MISMATCH, INSTANCE_ACCESS_BEFORE_SUPER_CALL!>y<!>)

    val y: Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, secondaryConstructor,
stringLiteral */
