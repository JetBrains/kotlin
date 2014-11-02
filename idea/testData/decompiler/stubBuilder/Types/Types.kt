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
}