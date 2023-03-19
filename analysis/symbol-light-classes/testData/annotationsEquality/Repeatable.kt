// PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForAnnotationClass
// EXPECTED: java.lang.annotation.Repeatable
// EXPECTED: kotlin.annotation.Repeatable

@Repeatable
annotation class <caret>One(val value: String)