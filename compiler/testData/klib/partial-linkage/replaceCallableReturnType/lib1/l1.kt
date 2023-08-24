class Foo(val seed: Int) {
    override fun toString() = "Foo[$seed]"
    fun doCommon() = seed - 1
    fun doSpecificFoo() = seed - 1
}

class Bar(val seed: Int) {
    override fun toString() = "Bar[$seed]"
    fun doCommon() = seed + 1
    fun doSpecificBar() = seed + 1
}

val topLevelProperty = Foo(42)
fun topLevelFunction() = Foo(42)
