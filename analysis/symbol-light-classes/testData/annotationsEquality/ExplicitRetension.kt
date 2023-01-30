// PSI: org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForAnnotationClass
// EXPECTED: java.lang.annotation.Retention
// EXPECTED: kotlin.annotation.Retention
// UNEXPECTED: java.lang.annotation.Target

@Retention(AnnotationRetention.RUNTIME)
annotation class ImplicitRete<caret>nsion
