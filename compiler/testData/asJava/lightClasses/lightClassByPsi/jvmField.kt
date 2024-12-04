class A {
    @JvmField
    val a: Collection<*> = emptyList()
    @JvmField
    var b: Int = 1

    companion object {
        @JvmField
        val c: Collection<*> = emptyList()
        @JvmField
        var d: Int = 1
    }
}

interface B {
    companion object {
        @JvmField
        val a: Collection<*> = emptyList()
    }
}

class C(
    @JvmField
    val a: Collection<*> = emptyList(),
    @JvmField
    var b: Int = 1
)
// COMPILATION_ERRORS
