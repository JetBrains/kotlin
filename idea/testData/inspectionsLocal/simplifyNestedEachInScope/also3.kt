// WITH_RUNTIME
fun test(){
    listOf(1,2,3).also<caret> { s -> s.forEach{ it + 4 } }.forEach{ it + 5 }
}