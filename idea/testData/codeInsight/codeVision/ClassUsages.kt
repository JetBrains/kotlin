// MODE: usages

<# block [ 5 Usages] #>
open class SomeClass {}
class SomeOtherClass : SomeClass {} // <== (1): class extension
class SomeYetOtherClass : SomeClass { // <== (2): class extension
<# block [     1 Usage] #>
    fun acceptsClass(param: SomeClass) {} // <== (3): parameter type
    fun returnsInterface(): SomeClass {} // <== (4): return type
    fun main() = acceptsClass(object : SomeClass {}) // <== (5): anonymous class instance
}