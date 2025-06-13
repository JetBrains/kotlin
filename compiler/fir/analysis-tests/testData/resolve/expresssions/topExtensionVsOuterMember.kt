// RUN_PIPELINE_TILL: BACKEND
fun Outer.Inner.foo() = 42

class Outer {

    fun foo() = ""

    inner class Inner {
        val x = foo() // Should be Int
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inner, integerLiteral,
propertyDeclaration, stringLiteral */
