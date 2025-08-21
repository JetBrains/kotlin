// RUN_PIPELINE_TILL: FRONTEND
class Container<A> {
    fun consume(arg: A) {}
}

fun <B> build(func: (Container<B>) -> B) {}

fun main(b: Boolean) {
    build { container ->
        if (b) {
            return@build <!CANNOT_INFER_IT_PARAMETER_TYPE!>{ <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>arg<!> ->
                arg.<!UNRESOLVED_REFERENCE!>length<!>
            }<!>
        }
        container.consume({ arg: String -> })
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, ifExpression, lambdaLiteral, nullableType,
typeParameter */
