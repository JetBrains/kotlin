// MODULE: lib
// FILE: A.kt
class A {
    var privateVar = 12
        private set

    private inline fun privateSetVar1(value: Int) {
        privateVar = value
    }


    private inline fun privateSetVar2(value: Int) {
        privateSetVar1(value)
    }

    internal inline fun internalSetVar(value: Int) {
        privateSetVar2(value)
    }
}


internal fun topLevelGet(a: A) = a.privateVar

internal fun topLevelSet(a: A, value: Int) {
    a.internalSetVar(value)
}

internal inline fun topLevelInlineGet(a: A) = a.privateVar

internal inline fun topLevelInlineSet(a: A, value: Int) {
    a.internalSetVar(value)
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    val a = A()
    result += a.privateVar
    a.internalSetVar(3)
    result += a.privateVar
    topLevelSet(a, 4)
    result += topLevelGet(a)
    topLevelInlineSet(a, 5)
    result += topLevelInlineGet(a)
    if (result != 24) return result.toString()
    return "OK"
}
