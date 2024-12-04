// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

enum class E {
    A {
        override val foo: Int = 65
    };

    abstract val foo: Int?
}

// enum_entry_initializer: test/E.A
