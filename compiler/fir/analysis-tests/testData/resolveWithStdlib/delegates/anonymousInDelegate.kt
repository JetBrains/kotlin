interface Foo {
    fun bar(): Int
}

val x by lazy {
    val foo = object : Foo {
        override fun bar(): Int = 42
    }
    foo.bar()
}