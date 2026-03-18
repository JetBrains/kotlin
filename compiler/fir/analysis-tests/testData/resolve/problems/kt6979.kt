// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-6979

// KT-6979: Sam constructor should be a constructor, not a function (from compiler point of view)

fun interface Foo {
    fun invoke(): String
}

fun interface Bar {
    fun invoke(): Int
}

fun test() {
    val foo = Foo { "hello" }
    val bar = Bar { 42 }
    foo.invoke()
    bar.invoke()
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, integerLiteral, interfaceDeclaration, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */
