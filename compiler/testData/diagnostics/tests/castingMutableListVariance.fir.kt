// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77444
// WITH_STDLIB

fun main() {
    test(mutableListOf<String>())
}

fun test(list: Any) {
    if (list is MutableList<out Any?>) {
        val list2 = list as MutableList<Any?>
        list2.add(null)
    }
}

fun fest(list: Any) {
    if (list is MutableList<out Any?>) {
        list is MutableList<Any?>
    }
}
