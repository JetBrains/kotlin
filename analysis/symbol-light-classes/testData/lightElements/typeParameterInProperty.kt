// From getter:
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// From setter:
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)

interface KtInterface {
    context(x: TT)
    var <T<caret>T> TT.foo: TT
        get() = this
        set(value) {}
}

