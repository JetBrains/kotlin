// WITH_DEFAULT_VALUE: false
suspend fun foo(n: Int) = n

suspend fun test() {
    val m = <selection>foo(1)</selection>
}