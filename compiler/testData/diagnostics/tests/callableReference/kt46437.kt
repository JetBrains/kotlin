// RUN_PIPELINE_TILL: FRONTEND
fun box(): String {
    if (true) <!INVISIBLE_MEMBER!>X::y<!> else null
    return "OK"
}

object X {
    private val y = null
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, nullableType, objectDeclaration, propertyDeclaration,
stringLiteral */
