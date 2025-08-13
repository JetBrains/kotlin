// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-51634

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
