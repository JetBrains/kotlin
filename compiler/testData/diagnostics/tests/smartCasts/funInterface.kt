// RUN_PIPELINE_TILL: BACKEND
fun interface LLElementMapper : (String) -> Int?

fun foo(): LLElementMapper? = null

fun bar(y: LLElementMapper): LLElementMapper {
    val x = foo()
    if (x != null) {
        return x
    }

    return y
}

/* GENERATED_FIR_TAGS: equalityExpression, funInterface, functionDeclaration, functionalType, ifExpression,
interfaceDeclaration, localProperty, nullableType, propertyDeclaration, samConversion */
