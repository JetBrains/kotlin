// ISSUE: KT-46288

fun (suspend () -> Unit).extensionFunc() {}
fun parameterFunc(func: suspend () -> Unit) {}
fun testFunc() {}

fun main() {
    parameterFunc(::testFunc)
    (::testFunc).extensionFunc()
}
