package test

fun foo(): String {
    fun bar(): String {
        return ""   // test/TestPackage$foo$1
    }
    return bar()
}
