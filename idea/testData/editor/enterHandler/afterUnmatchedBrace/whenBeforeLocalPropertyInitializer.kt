fun test() {
    val test = when { <caret>1
}
//-----
fun test() {
    val test = when { 
        <caret>1
    }
}