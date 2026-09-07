// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getBar)
@get:JvmName("getBar")
val <caret>p: Int
    get() = 42