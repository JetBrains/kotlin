// WITH_STDLIB
// ISSUE: KT-41679

fun test_1() {
    var y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<{Comparable<*> & java.io.Serializable}>")!>mutableListOf("MH", 19, true)<!>
    y[0] = "value4"
}

fun test_2(x: Int) {
    var y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<{Comparable<*> & java.io.Serializable}>")!>mutableListOf("MH", x, true)<!>
    y[0] = "value4"
}
