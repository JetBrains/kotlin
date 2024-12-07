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

internal inline fun customSetVarA(value: Int) {
    privateSetterVarA = value
}

internal inline fun customSetVarB(value: Int) {
    privateSetterVarB = value
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += privateSetterVarA
    result += privateSetterVarB
    customSetVarA(42)
    customSetVarB(21)
    result += privateSetterVarA
    if (result != 42) return result.toString()
    return "OK"
}
