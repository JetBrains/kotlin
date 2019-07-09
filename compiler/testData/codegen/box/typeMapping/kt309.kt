// TARGET_BACKEND: JVM

// WITH_RUNTIME

class N {
    fun foo() = null
}

fun box(): String {
    val method = N::class.java.getDeclaredMethod("foo")
    if (method.returnType.name != "java.lang.Void") return "Fail: Nothing should be mapped to Void"
    
    return "OK"
}
