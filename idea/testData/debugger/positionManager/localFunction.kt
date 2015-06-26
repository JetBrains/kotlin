package test

fun foo(): String {
    fun bar(): String {
        return ""   // test/LocalFunction\$foo\$1
    }
    return bar()
}
