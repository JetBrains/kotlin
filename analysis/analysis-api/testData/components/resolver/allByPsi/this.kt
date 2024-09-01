package one

class MyClass {
    fun foo() {}

    init {
        foo()
        this.foo()
        this@MyClass.foo()
    }

    inner class InnerOuter {
        fun foo() {}

        init {
            foo()
            this.foo()
            this@InnerOuter.foo()
            this@MyClass.foo()
        }

        inner class InnerNested {
            fun foo() {}

            init {
                foo()
                this.foo()
                this@InnerNested.foo()
                this@InnerOuter.foo()
                this@MyClass.foo()

                val f = anon@ fun MyClass.(s: String) {
                    foo()
                    this.foo()
                    this@anon.foo()
                    this@InnerNested.foo()
                    this@InnerOuter.foo()
                    this@MyClass.foo()
                }

                myWith { t ->
                    foo()
                    this.foo()
                    this@myWith.foo()
                    this@InnerNested.foo()
                    this@InnerOuter.foo()
                    this@MyClass.foo()
                }

                myWith label@{
                    this@label.foo()
                }
            }
        }
    }
}

fun <T> T.myWith(body: T.(T) -> Unit) {
    body(this)
}
