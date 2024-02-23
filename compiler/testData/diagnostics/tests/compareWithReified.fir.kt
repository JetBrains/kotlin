// ISSUE: KT-66005

inline fun <reified T> foo(v: T) {
    T == Int
    // This is a comparison of companion objects
    Int == Int
}
