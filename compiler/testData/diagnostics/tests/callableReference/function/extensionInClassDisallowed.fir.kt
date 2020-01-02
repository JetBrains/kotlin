// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
class A {
    fun Int.extInt() = 42
    fun A.extA(x: String) = x
    
    fun main() {
        Int::extInt
        A::extA

        eat(Int::extInt)
        eat(A::extA)
    }
}

fun eat(value: Any) {}

fun main() {
    A::extInt
    A::extA
}
