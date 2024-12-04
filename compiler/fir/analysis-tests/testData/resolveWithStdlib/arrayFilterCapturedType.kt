// RUN_PIPELINE_TILL: BACKEND
fun test(elements: Array<out String?>) {
    val filtered = elements.filterNotNull()
}
