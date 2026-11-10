// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(component3)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(component3)
data class C(val a: Int, val b: String) {
    fun component<caret>3(): Int = 3
}
