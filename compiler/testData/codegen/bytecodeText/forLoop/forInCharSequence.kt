// IGNORE_BACKEND: JVM_IR
fun test() {
    var s = ""
    for (c in "testString") {
        s += c
    }
    for (c in StringBuilder("testStringBuilder")) {
        s += c
    }
    for (c in "testCharSequence".apply<CharSequence>{}) {
        s += c
    }
}

// 0 iterator
// 0 hasNext
// 0 nextChar
