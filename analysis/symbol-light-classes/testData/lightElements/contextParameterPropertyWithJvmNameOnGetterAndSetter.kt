// LANGUAGE: +ContextParameters
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(customGetter)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(customSetter)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(customGetter)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(customGetter)
class C {
    context(c: Int)
    @get:JvmName("customGetter")
    @set:JvmName("customSetter")
    var va<caret>lue: String
        get() = ""
        set(v) {}
}
