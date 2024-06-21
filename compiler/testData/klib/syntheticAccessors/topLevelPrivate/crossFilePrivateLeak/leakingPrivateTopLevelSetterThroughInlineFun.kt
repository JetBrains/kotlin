// IGNORE_BACKEND: NATIVE

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

// FILE: main.kt
fun box(): String {
    var result = 0
    result += privateSetterVarA
    result += privateSetterVarB
    customSetVarA(42)
    customSetVarB(21)
    result += privateSetterVarA
    if (result != 84) return result.toString()
    return "OK"
}
