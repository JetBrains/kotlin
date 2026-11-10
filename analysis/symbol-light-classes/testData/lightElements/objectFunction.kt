// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(second)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(second)
object O {
    fun first() {}
    fun sec<caret>ond(p: Int) {}
    fun second() {}
}
