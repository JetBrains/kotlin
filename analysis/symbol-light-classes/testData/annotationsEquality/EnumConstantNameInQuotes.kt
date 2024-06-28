// PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
// EXPECTED: p.Anno
package p

@Anno(E.`TA-DA`)
class Fo<caret>o {}

enum class E {`TA-DA`, `TA-TA` }
annotation class Anno(val value: E)