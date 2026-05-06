// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-65997
// WITH_STDLIB
// DIAGNOSTICS: -UNCHECKED_CAST
// LANGUAGE: -DiscriminateNothingAsNullabilityConstraintInInference
// FIR_DUMP

fun <T> Any?.unsafeCast(): T = this as T

fun <R> foo(returnType: String): R {
    return when {
        returnType == "Nothing" -> throw Exception()
        else -> null.unsafeCast()
    }
}

/* GENERATED_FIR_TAGS: asExpression, equalityExpression, funWithExtensionReceiver, functionDeclaration, nullableType,
stringLiteral, thisExpression, typeParameter, whenExpression */
