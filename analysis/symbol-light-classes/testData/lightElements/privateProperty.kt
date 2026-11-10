// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(value)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(value)
class C {
    private val va<caret>lue: String = ""
}
