// RUN_PIPELINE_TILL: BACKEND
interface Base {
    val x: Int
}

open class Impl(override val x: Int) : Base {
    init {
        if (this.x != 0) foo()
    }
}

fun foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, init, integerLiteral,
interfaceDeclaration, override, primaryConstructor, propertyDeclaration, thisExpression */
