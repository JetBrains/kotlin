// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

class N {
    fun foo() = null
}

fun box(): String {
    val method = N::class.java.getDeclaredMethod("foo")
    if (method.returnType.name != "java.lang.Void") return "Fail: Nothing should be mapped to Void"
    
    return "OK"
}
