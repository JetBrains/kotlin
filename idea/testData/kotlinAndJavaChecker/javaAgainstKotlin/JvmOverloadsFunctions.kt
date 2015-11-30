package test.kotlin

interface A

@kotlin.jvm.JvmOverloads
public fun <T : A> foo(k: Class<T>, a: A, b: Boolean = false, s: String="hello"): List<T> {
    println("$b $s")
    return listOf()
}
