package util

annotation class Anno(val position: String)

class Foo {
    data class Pair(val a: Int, val b: Int)

    @Anno("destr $prop")
    v<caret>al (@Anno("a $prop") a, @Anno("b $prop") b) = Pair(0, 1)

    component object {
        const val prop = "str"
    }
}
