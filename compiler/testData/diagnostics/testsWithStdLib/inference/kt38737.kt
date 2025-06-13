// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
private const val dateRangeStart: String = "2020-01-01"
private const val dateRangeEnd: String = "2020-05-01"

private fun String?.toIconList(): List<String> = when (this) {
    null -> listOf("DATE_IS_NULL")
    in dateRangeStart..dateRangeEnd -> emptyList()
    else -> listOf("DATE_IS_OUT_OF_RANGE")
}

fun main() {
    println("2019-12-31".toIconList())
    println(null.toIconList())
}

/* GENERATED_FIR_TAGS: const, equalityExpression, funWithExtensionReceiver, functionDeclaration, nullableType,
propertyDeclaration, rangeExpression, smartcast, stringLiteral, whenExpression, whenWithSubject */
