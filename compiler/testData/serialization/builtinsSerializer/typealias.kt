package test

typealias TopLevel = String

fun testFun(x: TopLevel): TopLevel = x
val testVal: TopLevel = ""

class Outer {
    typealias NestedAlias = String

    fun testFun(x: NestedAlias): TopLevel = x
    val testVal: NestedAlias = ""

    class Nested {
        typealias NestedAlias = String

        fun testFun(x: NestedAlias): TopLevel = x
        val testVal: NestedAlias = ""
    }
}
