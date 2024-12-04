// PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
// EXPECTED: one.Anno
// UNEXPECTED: one.Anno2

package one

annotation class Anno

@Anno
fun regul<caret>arFunction() {}
