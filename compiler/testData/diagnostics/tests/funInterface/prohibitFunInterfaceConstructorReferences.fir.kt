fun interface Foo {
    fun run()
}

val x = ::Foo
val y = Foo { }
val z = ::Runnable