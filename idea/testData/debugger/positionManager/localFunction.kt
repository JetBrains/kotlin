package test

fun foo(): String {
    fun bar(): String {
        return ""   // test/LocalFunctionKt\$foo\$1
    }
    return bar()
}
