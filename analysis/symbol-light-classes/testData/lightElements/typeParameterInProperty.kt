// From getter:
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)
// From setter:
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameter(TT)

interface KtInterface {
    context(x: TT)
    var <T<caret>T> TT.foo: TT
        get() = this
        set(value) {}
}

