// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

annotation class Foo(val x: String)

companion const val String.bar = "ABC"

@Foo(String.bar)
fun baz() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral */
