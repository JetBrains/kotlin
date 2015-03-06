class Foo {
    fun bar(): String {
        fun foo<T>(t:() -> T) : T = t()
        foo { }
        return "OK"
    }
}

fun box(): String {
    return Foo().bar()
}
