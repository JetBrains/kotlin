// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74819
// WITH_STDLIB

fun foo(x: List<String>) {
    buildList {
        flatMap { listOf(it) }
        add("")
    }
}
