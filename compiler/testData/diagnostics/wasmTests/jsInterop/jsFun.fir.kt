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
