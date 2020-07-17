// MODE: usages

<# block [ 1 Usage] #>
interface SomeInterface {
<# block [     3 Usages] #>
    fun someFun(): String
    fun someOtherFun() = someFun() // <== (1): delegation from another interface method
    val someProperty = someFun() // <== (2): property initializer
}

fun main() {
    val instance = object: SomeInterface {
<# block [         1 Usage] #>
        override fun someFun(): String {} // <== (): used below
    }
    instance.someFun() <== (3): call on an instance
}