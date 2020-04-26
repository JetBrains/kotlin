open class SuperClass(val arg: () -> String)

object obj {

    fun foo(): String {
        return "OK"
    }

    class Foo : SuperClass(::foo)
}

fun box(): String {
    return obj.Foo().arg()
}


// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: BOUND_RECEIVER
