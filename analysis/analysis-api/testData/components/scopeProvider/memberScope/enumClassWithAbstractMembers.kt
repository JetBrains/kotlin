package test

enum class E {
    A {
        override val foo: Int = 65

        override fun bar() {
            println(foo)
        }
    },
    B {
        override val foo: Int? = null

        override fun bar() {
            println("Nothing to see here!")
        }
    };

    abstract val foo: Int?

    abstract fun bar()
}

// class: test/E
