fun test() {
    var s = ""
    for (c in "testString") {
        s += c
    }
}

// 0 iterator
// 0 hasNext
// 0 nextChar
// 0 INVOKEINTERFACE
// 1 charAt \(I\)C
// 1 length \(\)I
