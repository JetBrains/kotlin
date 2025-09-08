// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 12

    private inline fun privateSetVar1(value: Int) {
        privateVar = value
    }

    private inline fun privateGetVar1(): Int = privateVar

    private inline fun privateSetVar2(value: Int) {
        privateSetVar1(value)
    }

    private inline fun privateGetVar2(): Int = privateGetVar1()

    internal inline fun internalSetVar(value: Int) {
        privateSetVar2(value)
    }

    internal inline fun internalGetVar(): Int = privateGetVar2()
}

internal fun topLevelGet(a: A) = a.internalGetVar()

internal fun topLevelSet(a: A, value: Int) {
    a.internalSetVar(value)
}

internal inline fun topLevelInlineGet(a: A) = a.internalGetVar()

internal inline fun topLevelInlineSet(a: A, value: Int) {
    a.internalSetVar(value)
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    val a = A()
    result += a.internalGetVar()
    a.internalSetVar(3)
    result += a.internalGetVar()
    topLevelSet(a, 4)
    result += topLevelGet(a)
    topLevelInlineSet(a, 5)
    result += topLevelInlineGet(a)
    if (result != 24) return result.toString()
    return "OK"
}
