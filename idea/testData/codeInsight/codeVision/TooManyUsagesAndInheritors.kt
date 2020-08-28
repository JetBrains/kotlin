// MODE: usages-&-inheritors
// USAGES-LIMIT: 3
// INHERITORS-LIMIT: 2

<# block [ 3+ Usages   2+ Inheritors] #>
open class SomeClass {
    class NestedDerivedClass: SomeClass() {} // <== (1): nested class
}
<# block [ 1 Usage   1 Inheritor] #>
open class DerivedClass : SomeClass {} // <== (2): direct derived one
class AnotherDerivedClass : SomeClass {} // <== (3): yet another derived one
class DerivedDerivedClass : DerivedClass { // <== (): indirect inheritor of SomeClass
    fun main() {
        val someClassInstance = object : SomeClass() // { <== (4): anonymous derived one
            val somethingHere = ""
        }
    }
}