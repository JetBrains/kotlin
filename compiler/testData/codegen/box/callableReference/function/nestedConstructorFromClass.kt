class A {
    class Nested {
        val o = 111
        val k = 222
    }
    
    fun result() = (::Nested).let { it() }.o + (A::Nested).let { it() }.k
}

fun box(): String {
    val result = A().result()
    if (result != 333) return "Fail $result"
    return "OK"
}
