// !DUMP_CFG
class OuterClass {
    fun outerFunction() {}
    val outerProperty = 1
    val outerProperty2 = outerProperty

    class NestedClass {
        fun nestedFUnction() {}
        val nestedProperty = 1
        val nestedProperty2 = nestedProperty
    }
}