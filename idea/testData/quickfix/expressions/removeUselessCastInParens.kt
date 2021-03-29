// "Remove useless cast" "true"
fun test(x: Any): Int {
    if (x is String) {
        return (x <caret>as String).length
    }
    return -1
}

/* IGNORE_FIR */