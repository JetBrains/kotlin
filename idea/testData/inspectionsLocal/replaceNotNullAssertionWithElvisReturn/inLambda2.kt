// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<String>, number: Int?) {
    val x: List<Int> = list.map {
        number!!<caret>
    }
}