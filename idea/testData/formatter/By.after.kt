class Bar(val r: Runnable) : Runnable by r
class Foo {
    val bar by lazy { "" }
    val baz: String by lazy { "" }
}
