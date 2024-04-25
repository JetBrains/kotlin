// LANGUAGE: -AbstractClassMemberNotImplementedWithIntermediateAbstractClass
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS

var result = "Fail"

interface A<T> {
    fun f(x: T) {
        result = x.toString()
    }
}

abstract class B1<T> : A<T>

interface B2 {
    fun f(x: String)
}

class C : B1<String>(), B2

fun box(): String {
    (C() as B2).f("OK")
    return result
}
