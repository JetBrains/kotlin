// MODE: usages

<# block [ 5 Usages] #>
interface SomeInterface {}
interface SomeOtherInterface : SomeInterface {} // <== (1): interface extension
class SomeClass : SomeInterface { // <== (2): interface implementation
<# block [     1 Usage] #>
    fun acceptsInterface(param: SomeInterface) {} // <== (3): parameter type
    fun returnsInterface(): SomeInterface {} // <== (4): return type
    fun main() = acceptsInterface(object : SomeInterface {}) // <== (5): anonymous class instance
}