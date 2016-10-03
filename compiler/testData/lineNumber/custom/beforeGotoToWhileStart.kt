fun foo() {
    while (true) {
        if (testSome()) {
            break
        }
    }
}

fun testSome(): Boolean {
    return false
}

// 2 +3 4 2 7 10