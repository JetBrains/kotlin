// PROBLEM: none
// WITH_RUNTIME
fun test(){
    listOf(1,2,3).apply<caret> { 1.apply<caret> { forEach{ it + 1 } } }
}