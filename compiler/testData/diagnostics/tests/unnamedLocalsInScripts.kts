// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77252
// LANGUAGE: +UnnamedLocalVariables

fun foo(): Int = 123

val <!UNDERSCORE_IS_RESERVED!>_<!> = foo()

object Test {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, objectDeclaration, propertyDeclaration,
unnamedLocalVariable */
