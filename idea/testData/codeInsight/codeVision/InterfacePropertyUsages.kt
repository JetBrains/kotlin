// MODE: usages

<# block [ 1 Usage] #>
interface SomeInterface {
<# block [     2 Usages] #>
    val someProperty = "initialized"
    fun someFun() = "it's " + someProperty // <== (1):
}

fun main() {
    val instance = object: SomeInterface {}
    val someString = instance.someProperty // <== (2): call on an instance