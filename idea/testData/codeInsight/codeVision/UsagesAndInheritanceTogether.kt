// MODE: usages-&-inheritors

<# block [ 4 Usages   4 Inheritors] #>
open class SomeClass {
    class NestedDerivedClass: SomeClass() {} // <== (1): nested class
}
<# block [ 1 Usage   1 Inheritor] #>
open class DerivedClass : SomeClass {} // <== (2): direct derived one
class AnotherDerivedClass : SomeClass {} // <== (3): yet another derived one
class DerivedDerivedClass : DerivedClass { // <== (): indirect inheritor of SomeClass
    fun main() {
        val someClassInstance = object : SomeClass() { // <== (4): anonymous derived one
            val somethingHere = ""
        }
    }
}