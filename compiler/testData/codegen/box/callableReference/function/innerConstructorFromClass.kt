class A {
    inner class Inner {
        val o = 111
        val k = 222
    }
    
    fun result() = (A::Inner).let { it(this) }.o + (A::Inner).let { it(this) }.k
}

fun box(): String {
    val result = A().result()
    if (result != 333) return "Fail $result"
    return "OK"
}
