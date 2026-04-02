// RUN_PIPELINE_TILL: BACKEND
class My {
    val x: String

    constructor() {
        val y = bar(this)
        val z = foo()
        x = "$y$z"
    }

    fun foo() = x
}

fun bar(arg: My) = arg.x

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, localProperty, propertyDeclaration,
secondaryConstructor, thisExpression */
