package test

abstract class Types {
    val nullable: Int? = null
    abstract val list: List<Int>
    abstract val map: Map<Int, Int>
    abstract val nullableMap: Map<Int?, Int?>?
    abstract val projections: Map<in Int, out String>
    val function: () -> Unit = {}
    abstract val functionWithParam: (String, Int) -> List<String>
    abstract val extFunction: String.() -> List<String>
    abstract val extFunctionWithParam: String.(Int, String) -> List<String>

    abstract val extFunctionWithNullables: String.(Int?, String?) -> List<String?>?
    abstract val deepExtFunctionType: String.((Int) -> Int, String?) -> List<String?>?

    public fun <P1, P2, P3, R> Function3<P1, P2, P3, R>.extOnFunctionType() {
    }

    abstract val starList: List<*>
    abstract val starFun: Function1<*, *>
    abstract val extFun: @ExtensionFunctionType Function2<Int, Int, Unit>
    abstract val listExtStarFun: List<@ExtensionFunctionType Function1<*, *>>
    abstract val funTypeWithStarAndNonStar: Function1<*, Int>

    abstract fun functionTypeWithNamedArgs(fType: (first: String, second: Any?) -> Int)
}
