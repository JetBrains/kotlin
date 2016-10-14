// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'count{}'"
// IS_APPLICABLE_2: false
fun f11(list: List<Any?>): Int{
    var c = 0
    <caret>for (d in list) {
        if (d is String) {
            c++
        }
    }
    return c
}