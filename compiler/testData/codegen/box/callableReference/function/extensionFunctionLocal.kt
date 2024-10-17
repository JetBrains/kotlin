fun foo(): String {
    fun Int.bar(): String {
        return "O"
    }
    fun A<String>.bar(): String {
        return "K"
    }
    return runInt(Int::bar) + runA(A<String>::bar)
}

fun runInt(c: Int.() -> String) : String {
    return c(1)
}

class A<T>
fun runA(c: A<String>.() -> String) : String {
    return c(A<String>())
}

fun box(): String = foo()