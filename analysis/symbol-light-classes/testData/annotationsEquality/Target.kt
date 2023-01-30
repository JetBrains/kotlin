// PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForAnnotationClass
// EXPECTED: java.lang.annotation.Target
// EXPECTED: java.lang.annotation.Retention
// EXPECTED: kotlin.annotation.Target

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Annotatio<caret>nClass
