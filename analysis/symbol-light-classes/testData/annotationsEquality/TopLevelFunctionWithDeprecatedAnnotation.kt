// PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
// EXPECTED: kotlin.Deprecated
// UNEXPECTED: kotlin.jvm.JvmRecord
// EXPECTED: one.Anno
// UNEXPECTED: one.Anno2

package one

annotation class Anno

@Deprecated("") @Anno
fun regul<caret>arFunction() {}
