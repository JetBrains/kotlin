// LANGUAGE: +ContextReceivers
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
context(String)
fun function(x<caret>x: Int) {}
