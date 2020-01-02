class Test(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(i: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(i: Int) = 2
}

// KT-6413 Typechecker recursive problem when class have non-invariant generic parameters
class Test2<A, B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(i: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(i: Int) = 2
}

class Test3<in A, B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(i: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(i: Int) = 2
}

class Test4<A, out B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(i: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(i: Int) = 2
}

class Test5<A, out B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
    private val bas: Int = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(i: Int) = 2
    private fun bar2(): Int = bar2(1)
    private fun bar2(i: Int) = 2
}