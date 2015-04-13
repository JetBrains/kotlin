fun foo(s: String, i: Int){}
fun foo(c: Char){}

fun bar(b: Boolean, s: String){
    foo(if (b) <caret>xxx else "")
}

// ELEMENT: s
// CHAR: \t
