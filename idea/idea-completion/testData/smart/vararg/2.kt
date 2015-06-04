fun foo(vararg strings: String){ }

fun bar(s: String, arr: Array<String>){
    foo("", <caret>)
}

// EXIST: s
// ABSENT: arr
// ABSENT: *arr
