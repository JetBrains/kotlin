// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
// LANGUAGE: +CompanionBlocksAndExtensions
class C {
    companion {
        val ti<caret>tle: String = "C"
    }
}
