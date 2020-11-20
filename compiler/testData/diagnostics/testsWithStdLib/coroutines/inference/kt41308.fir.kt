// ISSUE: KT-41308

fun main() {
    sequence {
        val list: List<String>? = null
        val outputList = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>list ?: listOf()<!>
        yieldAll(outputList)
    }
}
