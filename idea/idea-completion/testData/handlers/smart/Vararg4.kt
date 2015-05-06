fun foo(vararg strings: String){ }

fun bar(arr: Array<String>){
    foo(<caret>)
}

// ELEMENT: arr
