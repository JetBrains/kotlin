// FIR_IDENTICAL
// ISSUE: KT-41308, KT-47830

fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>sequence<!> {
        val list: List<String>? = null
        val outputList = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>list ?: listOf()<!>
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NONE_APPLICABLE!>yieldAll<!>(outputList)
    }
}
