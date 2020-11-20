// PROBLEM: none
// WITH_RUNTIME
fun test(){
    val a = listOf(1,2,3)
    "".also<caret> { a.forEach { it + 2 } }
}