fun test() {
    foo()
    bar()
}

fun foo(i: Int = 1) {
}

inline fun bar(i: Int = 1) {
}

// 2 3 4 7 6 10 9
