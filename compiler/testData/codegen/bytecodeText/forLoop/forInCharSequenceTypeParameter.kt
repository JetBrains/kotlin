fun <T: CharSequence> test(sequence: T) {
    var s = ""
    for (c in sequence) {
        s += c
    }
}

// 0 iterator
// 0 hasNext
// 0 nextChar
