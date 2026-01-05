// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83382

fun error(): Nothing = null!!

class Foo {
    fun bar(): Int = error()
    val foo: String
    var baz: String
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, propertyDeclaration */
