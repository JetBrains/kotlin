// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(bar)
// EXPECTED_BY_PSI: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod(bar)
@JvmName("bar")
fun fo<caret>o() {}

fun foo(p: Int) {}
