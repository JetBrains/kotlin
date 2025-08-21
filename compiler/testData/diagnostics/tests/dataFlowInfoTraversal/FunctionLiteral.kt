// RUN_PIPELINE_TILL: FRONTEND
fun bar(x: Int) = x + 1

fun foo() {
    val x: Int? = null

    fun baz() = bar(<!TYPE_MISMATCH!>x<!>)
    fun quux() = if (x != null) bar(<!DEBUG_INFO_SMARTCAST!>x<!>) else baz()
    fun quuux() = bar(if (x == null) 0 else <!DEBUG_INFO_SMARTCAST!>x<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localFunction, localProperty, nullableType, propertyDeclaration, smartcast */
