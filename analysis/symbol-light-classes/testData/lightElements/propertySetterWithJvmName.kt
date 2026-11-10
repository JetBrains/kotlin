// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(customSetter)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(customGetter)
class C {
    var value: String = ""
        @JvmName("customGetter")
        get
        @JvmName("customSetter")
        s<caret>et
}
