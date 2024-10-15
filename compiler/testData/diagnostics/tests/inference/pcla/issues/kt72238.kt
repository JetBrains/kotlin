// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-72238

fun main() {
    buildList {
        var result: MutableList<Int>? = null
        for (i in 0..10) {
            result?.add(i) ?: run { result = mutableListOf(i) }

        }

        add("a")
    }
}
