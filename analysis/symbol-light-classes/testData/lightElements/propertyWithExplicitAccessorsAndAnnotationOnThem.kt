// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getBar)
val <caret>p: Int
    @JvmName("getBar")
    get() = 42