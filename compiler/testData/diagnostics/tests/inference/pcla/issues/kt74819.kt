// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74819
// WITH_STDLIB

fun foo(x: List<String>) {
    buildList {
        add("")
        flatMap { x }
    }
}
