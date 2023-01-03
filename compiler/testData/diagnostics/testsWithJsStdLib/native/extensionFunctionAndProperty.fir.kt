class A

external fun A.foo(): Unit = definedExternally

external var A.bar: String
    get() = definedExternally
    set(value) = definedExternally

external val A.baz: String
    get() = definedExternally
