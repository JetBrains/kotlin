fun foo(s: String?){}

fun foo(i: Int){}

fun bar(o: Any) {
    foo(o as <caret>)
}

// NUMBER: 2
// EXIST: String
// EXIST: Int
