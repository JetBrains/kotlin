@file:OptIn(ExperimentalVersionOverloading::class)

class GenericInnerOwner<T>(private val outerDefault: T) {
    inner class Inner {
        val value: String

        constructor(
            value: T,
            @IntroducedAt("1") fallback: T = outerDefault,
        ) {
            this.value = "$value/$fallback"
        }
    }
}

fun box(): String {
    if (GenericInnerOwner("O").Inner("I").value != "I/O") return "FAIL default"
    if (GenericInnerOwner("O").Inner("I", "K").value != "I/K") return "FAIL explicit"
    return "OK"
}
