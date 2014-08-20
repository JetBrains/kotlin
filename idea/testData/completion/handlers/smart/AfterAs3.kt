fun foo(l: List<String>){}
fun foo(l: List<String>, p: Int){}

fun bar(o: Any) {
    foo(o as <caret>)
}
