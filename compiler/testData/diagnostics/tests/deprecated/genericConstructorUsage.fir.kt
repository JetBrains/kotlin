// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER

open class C<T>() {
    @Deprecated("")
    constructor(p: Int) : this(){}
}

class D : C<String>(1)
