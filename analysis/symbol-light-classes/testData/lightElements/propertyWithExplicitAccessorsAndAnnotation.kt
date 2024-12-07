// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
@get:JvmName("getBar")
val <caret>p: Int
    get() = 42