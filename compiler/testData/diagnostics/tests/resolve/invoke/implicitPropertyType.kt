// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-57947

class A

fun A.bar() = baz("")

val baz = foo()

fun foo(): A.(String) -> Unit = TODO()

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
propertyDeclaration, stringLiteral, typeWithExtension */
