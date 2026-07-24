interface Foo {
    fun process(): Boolean
}

fun Any.test() {
    if (this is Foo) {
        println(<expr>this</expr>.process())
    }
}
