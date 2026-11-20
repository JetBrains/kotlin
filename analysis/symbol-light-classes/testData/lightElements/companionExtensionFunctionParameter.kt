// LANGUAGE: +CompanionBlocks +CompanionExtensions
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter(xx)
class Foo

companion fun Foo.function(x<caret>x: Int) {}
