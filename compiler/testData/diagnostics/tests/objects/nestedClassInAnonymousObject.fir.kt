class X {
    val foo = object {
        class Foo
    }

    fun test() {
        object {
            class Foo
        }
    }
}
