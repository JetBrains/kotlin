// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-51634
// DUMP_INFERENCE_LOGS: FIXATION

object A {
    fun <T> genericFunction(block: () -> T): T {
        return Any() <!UNCHECKED_CAST!>as? T<!> ?: block()
    }

    val property: Any = <!DEBUG_INFO_LEAKING_THIS!>genericFunction<!> {
        throw Exception()
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, functionalType, lambdaLiteral, nullableType,
objectDeclaration, propertyDeclaration, typeParameter */
