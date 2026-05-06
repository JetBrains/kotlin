// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-52219
// WITH_STDLIB

// KT-52219: False positive TYPE_MISMATCH in `when` with range in one of the branches

class MyDate(val year: Int, val month: Int, val day: Int) : Comparable<MyDate> {
    override fun compareTo(other: MyDate): Int =
        compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
}

fun getDateOrNull(): MyDate? = null

fun test(strings: List<String>) {
    val dates = strings.mapNotNull {
        when (getDateOrNull()) {
            null -> null
            in MyDate(1970, 1, 1)..MyDate(2023, 1, 1) -> it
            else -> null
        }
    }
}

fun testWithSubject(date: MyDate?) {
    when (date) {
        null -> null
        in MyDate(1970, 1, 1)..MyDate(2023, 1, 1) -> "in range"
        else -> null
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, operator, override, primaryConstructor, propertyDeclaration, rangeExpression, smartcast,
starProjection, stringLiteral, thisExpression, whenExpression, whenWithSubject */
