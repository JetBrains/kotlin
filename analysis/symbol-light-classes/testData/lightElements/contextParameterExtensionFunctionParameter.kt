// LANGUAGE: +ContextParameters
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
context(cc: String)
fun Int.function(x<caret>x: Int) {}
