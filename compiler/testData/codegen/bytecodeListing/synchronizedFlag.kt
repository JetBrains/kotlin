// WITH_STDLIB

@Synchronized
@JvmOverloads
fun testJvmOverloads(a: Int = 0) {}

class C {
    @Synchronized
    private fun testAccessor() {}

    fun lambda() = { -> testAccessor() }

    companion object {
        @Synchronized
        @JvmStatic
        fun testJvmStatic() {}
    }
}

inline class IC(val x: Int) {
    @Synchronized
    fun testInlineClassFun() {}
}