interface Base {
    var baz: String
    fun baz(): Boolean
}

abstract class Test : Base {
    var foo = 1
    abstract fun foo()
}

fun Any.test() {
    // First smartcast
    if (this is Base) {
        this.baz = baz
        baz = baz
        baz()

        // Nested smartcast
        if (this is Test) {
            this.baz = baz
            baz = baz

            this.foo = foo
            foo = foo

            foo()
        }
   }
}
