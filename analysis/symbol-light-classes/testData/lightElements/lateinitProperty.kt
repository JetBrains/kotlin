// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setValue)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(value)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(setValue)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(value)
class C {
    lateinit var va<caret>lue: String
}
