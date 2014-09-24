package test

fun foo(): String {
    fun bar(): String {
        return ""   // test/TestPackage\$localFunction\$.+\$foo\$1
    }
    return bar()
}
