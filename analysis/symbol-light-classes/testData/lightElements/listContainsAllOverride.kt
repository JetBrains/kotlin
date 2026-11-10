// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedKotlinCollectionMethod(containsAll)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedKotlinCollectionMethod(containsAll)
abstract class C : List<String> {
    override fun contains(element: String): Boolean = false
    override fun contains<caret>All(elements: Collection<String>): Boolean = false
}
