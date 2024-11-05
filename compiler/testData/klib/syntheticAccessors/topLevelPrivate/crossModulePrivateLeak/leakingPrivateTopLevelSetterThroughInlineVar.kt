// KT-72862: No property accessor found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// JVM_ABI_K1_K2_DIFF
// MODULE: lib
// FILE: a.kt
var privateSetterVarA = 0
    private set

var privateSetterVarB = 0
    private set(value){
        field = value * 2
    }

internal inline var inlineVarA: Int
    get() = privateSetterVarA
    set(value) {
        privateSetterVarA = value
    }

internal inline var inlineVarB: Int
    get() = privateSetterVarB
    set(value) {
        privateSetterVarB = value
    }

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += inlineVarA
    result += inlineVarB
    inlineVarA = 42
    inlineVarB = 21
    result += inlineVarA
    result += inlineVarB
    if (result != 84) return result.toString()
    return "OK"
}
