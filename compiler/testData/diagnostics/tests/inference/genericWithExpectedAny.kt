// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-51634
// WITH_STDLIB
// DIAGNOSTICS: -UNCHECKED_CAST
// LANGUAGE: +DiscriminateNothingAsNullabilityConstraintInInference
// FIR_DUMP

object A {
    fun <T> genericFunction(block: () -> T): T {
        return Any() as? T ?: block()
    }

    val property: Any = genericFunction {
        throw Exception()
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, functionalType, lambdaLiteral, nullableType,
objectDeclaration, propertyDeclaration, typeParameter */
