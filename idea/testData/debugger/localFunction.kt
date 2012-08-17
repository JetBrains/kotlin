package test

fun foo(): String {
    fun bar(): String {
        return ""   // test/namespace$foo$1
    }
    return bar()
}
