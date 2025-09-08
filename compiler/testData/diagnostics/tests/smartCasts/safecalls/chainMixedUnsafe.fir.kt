// RUN_PIPELINE_TILL: FRONTEND
fun calc(x: List<String>?): Int {
    // x is not-null only inside subList
    x?.subList(0, x.size - 1)<!UNSAFE_CALL!>.<!>get(x<!UNSAFE_CALL!>.<!>size)
    return x!!.size
}

/* GENERATED_FIR_TAGS: additiveExpression, checkNotNullCall, functionDeclaration, integerLiteral, nullableType, safeCall,
smartcast */
