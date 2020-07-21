// WITH_RUNTIME
fun test(){
    listOf(1,2,3).apply<caret> { this@apply.forEach { it + 5 } }
}