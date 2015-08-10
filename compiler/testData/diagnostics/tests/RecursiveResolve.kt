class Test(foo: Any?, bar: Any?) {
    val foo = foo ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bar = bar ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(<!UNUSED_PARAMETER!>i<!>: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(<!UNUSED_PARAMETER!>i<!>: Int) = 2
}

// KT-6413 Typechecker recursive problem when class have non-invariant generic parameters
class Test2<A, B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bar = bar ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(<!UNUSED_PARAMETER!>i<!>: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(<!UNUSED_PARAMETER!>i<!>: Int) = 2
}

class Test3<in A, B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bar = bar ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bas = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bas<!>()<!>
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(<!UNUSED_PARAMETER!>i<!>: Int) = 2
    private fun bar2() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!>(1)<!>
    private fun bar2(<!UNUSED_PARAMETER!>i<!>: Int) = 2
}

class Test4<A, out B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bar = bar ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bas = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bas<!>()<!>
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(<!UNUSED_PARAMETER!>i<!>: Int) = 2
    private fun bar2() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!>(1)<!>
    private fun bar2(<!UNUSED_PARAMETER!>i<!>: Int) = 2
}

class Test5<A, out B, C>(foo: Any?, bar: Any?) {
    val foo = foo ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bar = bar ?: <!DEBUG_INFO_LEAKING_THIS!>this<!>
    private val bas: Int = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(<!UNUSED_PARAMETER!>i<!>: Int) = 2
    private fun bar2(): Int = bar2(1)
    private fun bar2(<!UNUSED_PARAMETER!>i<!>: Int) = 2
}