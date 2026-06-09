// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: WASM, WASM_JS, WASM_WASI, NATIVE

// MODULE: lib
// FILE: lib.kt
var grandparentInitialized = false
var parentInitialized = false

open class Grandparent {
    companion {
        val grandparentProp: String = run {
            grandparentInitialized = true
            "grandparent"
        }
    }
}

open class Parent : Grandparent() {
    companion {
        val parentProp: String = run {
            parentInitialized = true
            "parent"
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt
class Child : Parent() {
    companion {
        fun hi(): String { return "hi" }
    }
}

fun box(): String {
    // The test checks that static_init can successfully reference static_init from a super class defined in a separate module

    val greeting = Child.hi()
    if (greeting != "hi") return "FAIL: greeting=$greeting"

    if (Parent.parentProp != "parent") return "FAIL: parentProp=${Parent.parentProp}"
    if (Grandparent.grandparentProp != "grandparent") return "FAIL: grandparentProp=${Grandparent.grandparentProp}"
    if (!parentInitialized) return "FAIL: parentInitialized=$parentInitialized"
    if (!grandparentInitialized) return "FAIL: grandparentInitialized=$grandparentInitialized"

    return "OK"
}
