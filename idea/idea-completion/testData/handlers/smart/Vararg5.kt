fun foo(vararg strings: String){ }

fun bar(list: List<String>){
    foo(list.<caret>)
}

// ELEMENT: toTypedArray
