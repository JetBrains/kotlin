// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getSecond)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setSecond)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(second)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getSecond)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setSecond)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(second)
fun test() {
    val o = object {
        var first: Int = 0
        var sec<caret>ond: Int = 1
    }
}
