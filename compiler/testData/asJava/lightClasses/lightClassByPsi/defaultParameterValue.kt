// Foo
class Foo(
    val z: Boolean = true,
    val b: Byte = 1.toByte(),
    val c: Char = 'c',
    val c2: Char = '\n',
    val sh: Short = 10.toShort(),
    val i: Int = 10,
    val l: Long = -10L,
    val f: Float = 1.0f,
    val d: Double = -1.0,
    val s: String = "foo",
    val iarr: IntArray = intArrayOf(1, 2, 3),
    val larr: LongArray = longArrayOf(-1L, 0L, 1L),
    val darr: DoubleArray = doubleArrayOf(7.3),
    val sarr: Array<String> = arrayOf("a", "bc"),
    val cl: Class<*> = Foo::class.java,
    val clarr: Array<Class<*>> = arrayOf(Foo::class.java),
    val em: Em = Em.BAR,
    val emarr: Array<Em> = arrayOf(Em.FOO, Em.BAR)
) {
    fun foo(a: Int = 5) {}
}

enum class Em {
    FOO, BAR
}
