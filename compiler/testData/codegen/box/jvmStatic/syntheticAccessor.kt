// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

class C {
    companion object {
        private @JvmStatic fun foo(): String {
            return "OK"
        }
    }

    fun bar(): String {
        return foo()
    }
}

fun box(): String {
    return C().bar()
}
