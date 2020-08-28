// MODE: usages

<# block [ 1 Usage] #>
abstract class SomeClass {
<# block [     3 Usages] #>
    abstract fun someFun(): String
    fun someOtherFun() = someFun() // <== (1): delegation from another method
    val someProperty = someFun() // <== (2): property initializer
}

fun main() {
    val instance = object: SomeClass {
<# block [         1 Usage] #>
        override fun someFun(): String {} // <== (): used below
    }
    instance.someFun() <== (3): call on an instance
}