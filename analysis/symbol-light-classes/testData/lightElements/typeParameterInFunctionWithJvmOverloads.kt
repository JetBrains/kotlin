// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)

@JvmOverloads()
fun <T<caret>T> foo(x: Int = 10, y: String = "", z: Boolean) {}

