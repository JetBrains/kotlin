// PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForAnnotationClass
// EXPECTED: kotlin.annotation.Target

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Annotatio<caret>nClass
