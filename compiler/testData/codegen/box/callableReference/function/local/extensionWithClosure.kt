class A

fun box(): String {
    var result = "Fail"

    fun A.ext() { result = "OK" }

    val f = A::ext
    f(A())
    return result
}
