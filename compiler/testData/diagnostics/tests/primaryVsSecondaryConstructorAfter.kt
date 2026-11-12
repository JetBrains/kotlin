// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ImprovedResolutionInSecondaryConstructors
// ISSUE: KT-77275
abstract class Super(x: String)

val y: String = ""

class A(x: String = y) : Super(y) {
    constructor(
        w: Int,
        z: Int,
        x: String = y,
    ) : this(y)

    val y: Int = 1
}

class B(x: String = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>z<!>) : Super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>z<!>) {
    constructor(
        w: Int,
        x: String = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>z<!>,
    ) : this(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>z<!>)

    val z: String = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, secondaryConstructor,
stringLiteral */
