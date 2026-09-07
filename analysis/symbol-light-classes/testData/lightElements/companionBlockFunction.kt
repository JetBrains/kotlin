// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(greet)
// LANGUAGE: +CompanionBlocks +CompanionExtensions
class C {
    companion {
        fun gre<caret>et(): String = "Hi"
    }
}
