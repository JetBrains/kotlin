// PROBLEM: none
// WITH_RUNTIME
fun test(){
    listOf(1, 2, 3).also<caret> { it.forEach { i ->
        if (i % 2 == 0) return@also
    }}
}