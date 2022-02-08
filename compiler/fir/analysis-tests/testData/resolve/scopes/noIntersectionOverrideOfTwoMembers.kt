// SCOPE_DUMP: C:foo;x, Explicit:foo;x, Implicit:foo;x

interface A {
    fun foo(): Any
    val x: Any
}

interface B : A {
    override fun foo(): Any
    override val x: Any
}

interface C : A, B

interface D {
    fun foo(): Int
    val x: Any
}

interface Explicit : C, D {
    override fun foo(): Int
    override val x: Any
}

interface Implicit : C, D
