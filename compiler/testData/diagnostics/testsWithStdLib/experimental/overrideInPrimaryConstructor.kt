// SKIP_TXT
// FIR_IDENTICAL

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class E

abstract class Foo {
    @E
    abstract val bar: String
}

class SubFoo(
    @OptIn(E::class)
    override val bar: String,
) : Foo()
