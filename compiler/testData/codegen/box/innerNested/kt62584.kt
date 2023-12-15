// TARGET_BACKEND: JVM
// WITH_STDLIB
class A<T>

val <R> A<R>.p: Any
    get() {
        open class Local<X>
        class AnotherLocal : Local<String>()
        return AnotherLocal()
    }

fun <R> A<R>.foo(): Any {
    open class Local<X>
    class AnotherLocal : Local<String>()
    return AnotherLocal()
}

fun box(): String {
    val signatureInProperty = A<String>().p::class.java.genericSuperclass.toString()
    if (signatureInProperty != "Kt62584Kt\$p\$Local<java.lang.String>") return signatureInProperty
    val signatureInFunction = A<String>().foo()::class.java.genericSuperclass.toString()
    if (signatureInFunction != "Kt62584Kt\$foo\$Local<java.lang.String>") return signatureInFunction
    return "OK"
}