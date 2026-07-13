// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun <A> combine(first: () -> A, second: () -> String): Int = 1
fun <B> combine(first: () -> Unit, second: () -> B): String = "(2)"

fun test() {
    val stringUnit = combine({ "" }, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringUnit<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>combine<!>({ Unit }, { "" })

    val unitUnit = combine({ Unit }, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitUnit<!>

    val stringString = combine({ "" }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringString<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>combine<!>({ TODO() }, { "" })

    val intString = combine({ 1 }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>intString<!>

    val stringInt = combine({ "" }) { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringInt<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>combine<!>({ Unit }) { "1" }

    combine<String>({}, { <!RETURN_TYPE_MISMATCH!>Unit<!> })
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
