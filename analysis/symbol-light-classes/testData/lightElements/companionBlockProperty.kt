// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(getTitle)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty(title)
// LANGUAGE: +CompanionBlocks +CompanionExtensions
class C {
    companion {
        val ti<caret>tle: String = "C"
    }
}
