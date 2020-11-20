// WITH_RUNTIME
fun test(){
    listOf(1,2,3).apply<caret> { forEach{ it + 4 } }.forEach{ it + 5 }
}