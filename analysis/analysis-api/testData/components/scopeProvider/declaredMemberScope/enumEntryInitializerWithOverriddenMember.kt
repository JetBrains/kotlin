package test

enum class E {
    A {
        override val foo: Int = 65
    };

    abstract val foo: Int?
}

// enum_entry_initializer: test/E.A
