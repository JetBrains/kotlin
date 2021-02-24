// MODE: usages

<# block [ 3 Usages] #>
fun function(param: String): Int = 1
<# block [ 1 Usage] #>
fun higherOrderFun(s: String, param: (String) -> Int) = param(s)

fun main() {
    function("someString")
    val functionRef = ::function
    higherOrderFun("someString", ::function)
}