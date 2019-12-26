fun box() {
    lookAtMe {
        42
    }
}

inline fun lookAtMe(f: () -> Int) {
    val a = 21
    a + f()
}
// IGNORE_BACKEND: JVM_IR
// 2 13 14 3 15 5 8 9 10