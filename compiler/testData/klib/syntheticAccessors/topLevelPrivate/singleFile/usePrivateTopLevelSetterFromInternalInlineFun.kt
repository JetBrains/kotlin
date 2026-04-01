var privateSetterVarA = 0
    private set

var privateSetterVarB = 0
    private set(value) {
        field = value * 2
    }

internal inline fun customSetVarA(value: Int) {
    privateSetterVarA = value
}

internal inline fun customSetVarB(value: Int) {
    privateSetterVarB = value
}

fun box(): String {
    var result = 0
    customSetVarA(42)
    result += privateSetterVarA
    customSetVarB(21)
    result += privateSetterVarB
    if (result != 84) return result.toString()
    return "OK"
}
