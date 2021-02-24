// FIR_COMPARISON
`// COMPILER_ARGUMENTS: -XXLanguage:+ReadDeserializedContracts -XXLanguage:+UseReturnsEffect

interface Foo {
    val x: Int

    fun f()
}

fun test(x: Any?) {
    require(x is Foo)
    x.<caret>
}

// EXIST: x
// EXIST: f
