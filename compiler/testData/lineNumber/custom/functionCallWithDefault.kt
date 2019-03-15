fun test() {
    foo()
    bar()
}

fun foo(i: Int = 1) {
}

inline fun bar(i: Int = 1) {
}
// IGNORE_BACKEND: JVM_IR
// 2 3 13 14 4 7 6 10 9 15