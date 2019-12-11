val nonConst = 1

const val constConst = nonConst * nonConst + 2

annotation class Ann(val x: Int, val y: String)

@Ann(nonConst, "${nonConst}")
fun foo1() {}

@Ann(nonConst + constConst, "${constConst}")
fun foo2() {}

annotation class ArrayAnn(val x: IntArray)

@ArrayAnn(intArrayOf(1, constConst, nonConst))
fun foo3() {}
