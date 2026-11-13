// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(value)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getValue)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(value)
class C {
    val va<caret>lue: Int = 1

    @JvmName("getValue2")
    fun getValue(): Int = 2
}
