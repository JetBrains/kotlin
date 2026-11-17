// LANGUAGE: +ContextParameters
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSetterParameter(value)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightContextParameter(cc)
context(cc: String)
var property: Int
    get() = 2
    set(va<caret>lue) {}
