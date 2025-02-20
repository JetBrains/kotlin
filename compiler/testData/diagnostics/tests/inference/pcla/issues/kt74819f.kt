// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74819
// WITH_STDLIB

fun foo(x: List<String>) =
    buildList {
        add("")
        addAll(flatMap { listOf(2) })
        addAll(flatMap { x })
    }
