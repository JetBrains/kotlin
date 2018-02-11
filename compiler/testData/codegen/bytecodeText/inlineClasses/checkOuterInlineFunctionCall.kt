// !LANGUAGE: +InlineClasses

inline class Foo(val a: String) {
    fun test(): String {
        return notInlineFun() + inlineFun()
    }
}

inline fun inlineFun(): String = "K"
fun notInlineFun(): String = "O"

// 0 INVOKESTATIC CheckOuterInlineFunctionCallKt.inlineFun
// 1 INVOKESTATIC CheckOuterInlineFunctionCallKt.notInlineFun