fun foo(s: String){}
fun foo(c: Char){}

fun bar(b: Boolean, s: String, c: Char){
    foo(if (b) "abc" else <caret>)
}

// EXIST: s
// ABSENT: c
// ABSENT: b
