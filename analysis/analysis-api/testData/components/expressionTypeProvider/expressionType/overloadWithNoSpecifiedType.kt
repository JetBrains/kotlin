interface Foo {
    fun foo(): Long
}

class Main : Foo {
    // [RETURN_TYPE_MISMATCH_ON_OVERRIDE]
    // Return type of 'fun foo(): Int' is not a subtype of
    // the return type of the overridden member 'fun foo(): Long' defined in 'Foo'.
    override fun foo() = 1
}

fun main() {
    <expr>Main().foo()</expr>
}
