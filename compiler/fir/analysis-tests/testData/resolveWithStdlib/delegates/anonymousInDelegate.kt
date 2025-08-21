// RUN_PIPELINE_TILL: BACKEND
interface Foo {
    fun bar(): Int
}

val x by lazy {
    val foo = object : Foo {
        override fun bar(): Int = 42
    }
    foo.bar()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, integerLiteral, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, override, propertyDeclaration, propertyDelegate */
