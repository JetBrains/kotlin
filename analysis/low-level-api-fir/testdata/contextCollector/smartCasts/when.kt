interface Foo {
    fun foo()
}

interface Bar {
    fun bar()
}

fun test(obj: Any) {
    when (obj) {
        is Foo -> obj.foo()
        is Bar -> <expr>obj.bar()</expr>
    }
}