// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@JvmName("sumInt")
fun <T1> Iterable<T1>.mySumOf(selector: (T1) -> Int): Int = 0
fun <T2> Iterable<T2>.mySumOf(selector: (T2) -> Long): Long = 0

fun main(x: List<String>) {
    val y1 = x.mySumOf { 0 }
    val y2 = x.mySumOf { 0L }
    val y3 = x.mySumOf { it.length }
    val y4 = x.mySumOf { it.length.toLong() }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>y2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>y4<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
