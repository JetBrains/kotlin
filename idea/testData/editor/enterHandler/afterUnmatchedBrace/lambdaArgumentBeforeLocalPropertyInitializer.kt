// WITH_RUNTIME
fun test() {
    val test = run { <caret>1
}
//-----
// WITH_RUNTIME
fun test() {
    val test = run { 
        <caret>1
    }
}