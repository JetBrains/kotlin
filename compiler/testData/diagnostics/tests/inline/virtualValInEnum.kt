enum class B2() {
    A {
        override val foo: String
            get() = "foo1"

        override val bar: String
            get() = "bar1"
    };

    open val foo
    <!DECLARATION_CANT_BE_INLINED_WARNING!>inline<!> get() = "foo"

    open <!DECLARATION_CANT_BE_INLINED_WARNING!>inline<!> val bar
        get() = "bar1"
}
