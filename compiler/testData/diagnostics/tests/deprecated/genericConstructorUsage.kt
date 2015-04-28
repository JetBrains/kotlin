// !DIAGNOSTICS: -UNUSED_EXPRESSION, -REFLECTION_TYPES_NOT_LOADED, -UNUSED_PARAMETER

open class C<T>() {
    deprecated("")
    constructor(p: Int) : this(){}
}

class D : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>C<String><!>(1)