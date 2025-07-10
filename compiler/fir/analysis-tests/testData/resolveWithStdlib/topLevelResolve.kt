// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

fun testMap() {
    val lst = listOf(42)
    val viaWith = with(lst) {
        map { it * it }
    }
}
