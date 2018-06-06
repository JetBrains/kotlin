// FLOW: IN

fun <caret>assignmentWithSum(n: Int): Int {
    var result = 0
    result += n
    result++
    --result
    return result
}