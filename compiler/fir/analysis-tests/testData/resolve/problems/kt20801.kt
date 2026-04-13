// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-20801

// KT-20801: USELESS_CAST diagnostic for complex expressions
var b: Boolean = true

fun namedFunction() = if (b) 42 as Int? else null
val functionLiteral = if (b) 42 as Int? else null
val property = if (b) 42 as Int? else null
val propertyWithAccessor: Int?
    get() = if (b) 42 as Int? else null

fun whenExpression(): Int? = when {
    b -> 42 as Int?
    else -> null
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, getter, ifExpression, integerLiteral, nullableType,
propertyDeclaration, whenExpression */
