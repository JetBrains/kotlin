// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Printer {
    fun x(vararg ar: String): String
}

class X {
    fun x(vararg arr: String): String = arr[0] + arr[1]
}

fun box(): String {
    val x: Printer = X()
    return x.x("O", "K")
}