// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

fun use(): String {
    @OptIn(ExperimentalAPI::class)
    for (i in 1..2) {
        function()
    }

    @OptIn(ExperimentalAPI::class)
    return function()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, forLoop, functionDeclaration, integerLiteral,
localProperty, propertyDeclaration, rangeExpression, stringLiteral */
