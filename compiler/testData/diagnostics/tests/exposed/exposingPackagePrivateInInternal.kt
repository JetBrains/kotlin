// RUN_PIPELINE_TILL: BACKEND
// FILE: Foo.java
class Foo {}

// FILE: test.kt
internal fun <T : Foo> Foo.bar(f: Foo): Foo = Foo()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, javaType */
