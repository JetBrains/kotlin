// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getBar)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(p)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getBar)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(p)
@get:JvmName("getBar")
val <caret>p: Int = 42