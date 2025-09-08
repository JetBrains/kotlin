// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface A {
    val foo: Int
    val bar: String
        get() = ""
}

fun test(foo: Int, bar: Int) {
    object : A {
        override val foo: Int = foo + bar
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, functionDeclaration, getter, interfaceDeclaration,
override, propertyDeclaration, stringLiteral */
