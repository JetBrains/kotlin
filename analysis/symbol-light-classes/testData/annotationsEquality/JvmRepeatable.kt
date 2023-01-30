// PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForAnnotationClass
// EXPECTED: java.lang.annotation.Repeatable
// UNEXPECTED: kotlin.annotation.Repeatable

@JvmRepeatable(TwoContainer::class)
annotation class T<caret>wo(val name: String)
annotation class TwoContainer(val value: Array<Two>)
