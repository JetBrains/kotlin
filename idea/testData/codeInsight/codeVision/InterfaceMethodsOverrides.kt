// MODE: inheritors

<# block [ 1 Implementation] #>
interface SomeInterface {
<# block [     1 Override] #>
    fun interfaceMethodA() = 10
}

class SomeClass : SomeInterface {
    override fun interfaceMethodA() = 20 // <== (1)
}