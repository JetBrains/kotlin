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

                val f = fun MyClass.(s: String) {
                    foo()
                    this.foo()
                    this@InnerNested.foo()
                    this@InnerOuter.foo()
                    this@MyClass.foo()
                }
            }
        }
    }
}