// WITH_STDLIB

annotation class A(val x: String)

fun foo(m: Map<String, Int>) {
    @A("foo/test")
    val test by lazy { 42 }
}
