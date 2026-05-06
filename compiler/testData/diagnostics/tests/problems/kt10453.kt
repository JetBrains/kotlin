// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-10453

// KT-10453: Can't infer generic when using some operators as operator calls

class Example {
    operator inline fun <reified T : Any> unaryPlus(): T = TODO()
    operator inline fun <reified T : Any> unaryMinus(): T = TODO()
    operator inline fun <reified T : Any> not(): T = TODO()
}

fun main() {
    val example = Example()

    val works: Boolean = example.unaryPlus()
    val alsoWorks: Boolean = example.unaryMinus()
    val andThisWorks: Boolean = example.not()

    val doesntWork: Boolean = +example
    val alsoDoesntWork: Boolean = -example
    val andThisDoesntWork: Boolean = !example
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, localProperty, operator, propertyDeclaration,
reified, typeConstraint, typeParameter, unaryExpression */
