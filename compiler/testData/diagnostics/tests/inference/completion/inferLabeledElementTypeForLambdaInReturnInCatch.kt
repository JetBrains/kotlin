// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-51009

fun test(b: Boolean, f: () -> String?): () -> String {
    val foo = try {
        f
    } catch (e: Exception) {
        return { "1" } // Infer return type of test () -> String instead of type of `f`
    }
    return { "2" }
}

fun test2(b: Boolean, f: () -> String?) = run { // implicit return type
    val foo = try {
        f
    } catch (e: Exception) {
        <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> { "1" }
    }
    { "2" }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, tryExpression */
