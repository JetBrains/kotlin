fun cond() = false

fun bar() {}

fun foo() {
    if (cond()) {
        bar()
    } else if (true) {
        bar()
    } else {
        bar()
    }

    if (false) {
        bar()
    } else if (true) {
        bar()
    } else {
        bar()
    }

    if (true) {
        bar()
    } else if (false) {
        bar()
    } else {
        bar()
    }
}

// 1 LINENUMBER 6
// 1 LINENUMBER 8
// 1 LINENUMBER 14
// 1 LINENUMBER 16
// 1 LINENUMBER 22
