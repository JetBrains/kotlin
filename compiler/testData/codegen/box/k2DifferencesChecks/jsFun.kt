// ORIGINAL: /compiler/testData/diagnostics/wasmTests/jsInterop/jsFun.fir.kt
// WITH_STDLIB
@JsFun("() => {}")
external fun topLevelExternalFun(): Unit

external class ExternalClass {
    @JsFun("() => {}")
    fun memberFun(): Unit
}

@JsFun("() => {}")
fun topLevelNonExternalFun(): Unit {}

class NonExternalClass {
    @JsFun("() => {}")
    fun memberFun(): Unit {}
}


fun box() = "OK"
