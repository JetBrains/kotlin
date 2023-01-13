interface IntConvertible {
    fun toInt(): Int
}

fun <FooTP> foo(init: Int, v: FooTP, l: Int.(FooTP) -> Int) = init.l(v)

fun <BarTP : IntConvertible> computeSum(array: Array<BarTP>) = foo(0, array) {
    var res = this
    for (element in it) res += element.toInt()
    res
}

class N(val v: Int) : IntConvertible {
    override fun toInt() = v
}

fun box(): String {
    if (computeSum(arrayOf(N(2), N(14))) != 16) return "Fail"
    return "OK"
}
