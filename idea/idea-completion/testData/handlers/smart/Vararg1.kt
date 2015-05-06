fun foo(vararg strings: String){ }

fun bar(s: String){
    foo(<caret>)
}

// ELEMENT: s
