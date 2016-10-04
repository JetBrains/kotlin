// WITH_RUNTIME

fun test(): String {
    var string = ""
    for (c in "foo") {
        string += c
    }

    return string
}

// 0 iterator
// 0 hasNext
// 0 next
