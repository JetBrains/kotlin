// TARGET_BACKEND: JVM

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
