// IGNORE_BACKEND: NATIVE

// MODULE: lib
// FILE: a.kt
private fun privateFun() = "OK"
private inline fun privateInlineFun1() = privateFun()
private inline fun privateInlineFun2() = privateInlineFun1()
private inline fun privateInlineFun3() = privateInlineFun2()
private inline fun privateInlineFun4() = privateInlineFun3()
internal inline fun internalInlineFun() = privateInlineFun4()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
