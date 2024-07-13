// IGNORE_BACKEND: NATIVE
// ^^^ Muted, because the IR node for accessing "outher this" in Native (IrGetField of `$this$0` field) is different from
//     the corresponding IR node in JS (IrGetValue of `<this>` value parameter). To be unmuted along with the fix of KT-67172.

class Outer {
    private fun privateMethod() = "OK"
    inner class Inner{
        internal inline fun internalMethod() = privateMethod()
    }
}

fun box(): String {
    return Outer().Inner().internalMethod()
}
