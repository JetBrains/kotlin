// FIR_IDENTICAL
fun useUnit0(fn: () -> Unit) {}
fun useUnit1(fn: (Int) -> Unit) {}

fun fn0() = 1
fun fn1(x: Int) = 1
fun fnv(vararg xs: Int) = 1

fun test0() = useUnit0(::fn0)
fun test1() = useUnit1(::fn1)
fun testV0() = useUnit0(::fnv)
fun testV1() = useUnit1(::fnv)
