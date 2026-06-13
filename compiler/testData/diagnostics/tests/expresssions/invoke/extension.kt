// RUN_PIPELINE_TILL: BACKEND


fun x() {}

class Foo {
    operator fun Int.invoke(): Foo = this@Foo

    val x = 0

    fun foo() = x() // should resolve to invoke
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, operator,
propertyDeclaration, thisExpression */
