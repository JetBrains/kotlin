// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74819
// WITH_STDLIB

// 4 variables
// E of buildList
// T, R of flatMap
// L of listOf
fun <L> listOf(arg: L): List<L> = TODO()

fun foo(x: List<String>) {
    buildList {
        flatMap { listOf(it) }
        add("")
    }
}
