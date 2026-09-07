// ISSUE: KT-87399
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(xLongPrefixGetter)
// EXPECTED: org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod(xLongPrefixGetter)

package a

class Target

@get:JvmName("xLongPrefixGetter")
@set:JvmName("xLongPrefixSetter")
var Target.va<caret>lue: String
    get() = ""
    set(v) {}

