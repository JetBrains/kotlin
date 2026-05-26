// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
// LANGUAGE: +CompanionBlocksAndExtensions
class C {
    companion {
        fun gre<caret>et(): String = "Hi"
    }
}
