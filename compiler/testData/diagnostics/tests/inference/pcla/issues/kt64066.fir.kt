// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-64066

fun box() {
    val map = buildMap {
        put(1, 1)
        for (v in values) {}
    }
}
