// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedKotlinCollectionMethod(contains)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(contains)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedKotlinCollectionMethod(contains)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(contains)
abstract class C : Collection<String> {
    override fun con<caret>tains(element: String): Boolean = false
    override fun containsAll(elements: Collection<String>): Boolean = false
}
