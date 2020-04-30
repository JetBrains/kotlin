// MODE: usages

<# block [ 1 Usage] #>
interface SomeClass {
<# block [     3 Usages] #>
    var someProperty = "initialized"
    fun someFun() = "it's " + someProperty // <== (1): reference from expression
}

fun main() {
    val instance = object: SomeClass {}
    val someString = instance.someProperty // <== (2): getter call
    instance.someProperty = "anotherValue" // <== (3): setter call