// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(foo)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(foo)
enum class E {
    A {
        override fun fo<caret>o() {}
        fun bar() {}
    },
    B;

    open fun foo() {}
}
