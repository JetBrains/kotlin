// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-14773
// WITH_STDLIB

// KT-14773: USELESS_ELVIS warning is not reported in presence of a "type mismatch" error
fun main(args: Array<String>) {
    fun foo(): List<Pair<String, String>> = listOf<Pair<String, String?>>("" to "").map {
        <!RETURN_TYPE_MISMATCH!>it.first to it.second <!USELESS_ELVIS!>?: "foo"<!><!>
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, lambdaLiteral, localFunction, nullableType, stringLiteral */
