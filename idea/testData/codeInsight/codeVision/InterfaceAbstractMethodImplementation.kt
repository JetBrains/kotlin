// MODE: inheritors

<# block [ 1 Implementation] #>
interface SomeInterface {
<# block [     2 Implementations] #>
    fun interfaceMethodA()
}
<# block [ 1 Inheritor] #>
open class SomeClass : SomeInterface {
<# block [     1 Override] #>
    override fun interfaceMethodA() {} // <== (1)
}

class SomeDerivedClass : SomeClass() {
    override fun interfaceMethodA() {} // <== (2)
}