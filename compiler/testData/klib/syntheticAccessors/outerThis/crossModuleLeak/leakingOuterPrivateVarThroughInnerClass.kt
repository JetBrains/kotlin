// IGNORE_BACKEND: NATIVE
// ^^^ Muted, because the IR node for accessing "outher this" in Native (IrGetField of `$this$0` field) is different from
//     the corresponding IR node in JS (IrGetValue of `<this>` value parameter). To be unmuted along with the fix of KT-67172.

// MODULE: lib
// FILE: Outer.kt
class Outer {
    private var privateVar = 20

    inner class Inner {
        internal inline fun customVarGetter() = privateVar
        internal inline fun customVarSetter(value: Int) {
            privateVar = value
        }
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    val inner = Outer().Inner()

    result += inner.customVarGetter()
    inner.customVarSetter(22)
    result += inner.customVarGetter()
    if (result != 42) return result.toString()
    return "OK"
}
