fun foo(vararg strings: String, optional: String = ""){ }

fun bar(arr: Array<String>){
    foo(<caret>)
}

// ELEMENT: arr
