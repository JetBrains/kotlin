// PROBLEM: none
// WITH_RUNTIME
fun test(){
    mutableListOf(1,2,3).also<caret> { list -> list.forEach { list.add(it) } }
}