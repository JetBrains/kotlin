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

interface Grouping<GroupingInputTP, out GroupingOutputTP> {
    fun keyOf(element: GroupingInputTP): GroupingOutputTP
}

fun <GroupingByTP> groupingBy(keySelector: (Char) -> GroupingByTP): Grouping<Char, GroupingByTP> {

    fun <T> foo(p0: T, p1: GroupingByTP) {}

    foo(0, keySelector('a'))

    class A<T>(p0: T, p1: GroupingByTP) {}

    A(0, keySelector('a'))

    return object : Grouping<Char, GroupingByTP> {
        override fun keyOf(element: Char): GroupingByTP = keySelector(element)
    }
}

class Delft<DelftTP> {
    fun getComparator(other: DelftTP) = { this == other }
}

fun box(): String {
    if (computeSum(arrayOf(N(2), N(14))) != 16) return "Fail1"

    if (groupingBy { it }.keyOf('A') != 'A') return "Fail2"
    return "OK"
}
