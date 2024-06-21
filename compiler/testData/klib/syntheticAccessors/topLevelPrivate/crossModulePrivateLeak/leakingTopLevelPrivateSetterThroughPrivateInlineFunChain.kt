// IGNORE_BACKEND: NATIVE

// MODULE: lib
// FILE: a.kt
var privateSetterVar = 12
    private set

private inline fun privateSetVar1(value: Int) {
    privateSetterVar = value
}

private inline fun privateGetVar1(): Int = privateSetterVar

private inline fun privateSetVar2(value: Int) {
    privateSetVar1(value)
}

private inline fun privateGetVar2(): Int = privateGetVar1()

internal inline fun internalSetVar(value: Int) {
    privateSetVar2(value)
}

internal inline fun internalGetVar(): Int = privateGetVar2()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += internalGetVar()
    internalSetVar(3)
    result += internalGetVar()
    if (result != 15) return result.toString()
    return "OK"
}
