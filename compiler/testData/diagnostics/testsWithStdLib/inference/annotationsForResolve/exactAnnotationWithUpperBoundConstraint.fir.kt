// !DIAGNOSTICS: -UNUSED_PARAMETER -DEBUG_INFO_CONSTANT -UNUSED_EXPRESSION

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <@kotlin.internal.OnlyInputTypes K, V, V1 : V?>
    Map<out K, @kotlin.internal.Exact V>.getOrDefault_Exact(key: K, defaultValue: V1): V1 = TODO()

fun test() {
    val map: Map<String, Int> = mapOf("x" to 1)

    val r1 = map.getOrDefault_Exact("y", null)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>r1<!>

    val r2 = map.getOrDefault_Exact("y", null as Int?)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>r2<!>

    map.getOrDefault_Exact("y", <!ARGUMENT_TYPE_MISMATCH!>"string"<!>)
}
