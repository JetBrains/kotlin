fun foo(param1: String, vararg param2: Int) { }

fun bar(arr: IntArray) {
    foo(param2 = <caret>)
}

// ELEMENT: arr
