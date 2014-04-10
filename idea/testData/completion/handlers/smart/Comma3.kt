fun foo(s: String){ }
fun foo(s: String, i: Int){ }

fun bar(sss: String) {
    foo(<caret>)
}

//ELEMENT: sss
