// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY

class A<T>(t: T) {
    companion {
        val a = t
    }
    //java.lang.RuntimeException: Exception while generating code
}

class B(t: String) {
    companion {
        val a = t
    }
}

fun box() = "OK"
