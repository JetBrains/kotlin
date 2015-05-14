fun foo(s: String?){}

fun foo(i: Int){}

fun bar(o: Any) {
    foo(o as <caret>)
}

// EXIST: String
// EXIST: Int
// NOTHING_ELSE: true
