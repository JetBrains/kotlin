// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(foo)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(foo)
@file:JvmMultifileClass
@file:JvmName("Utils")

fun fo<caret>o() {}

fun bar() {}
