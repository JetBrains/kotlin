/* NonReanalyzableNonClassDeclarationStructureElement */fun foo() {
    var x: Int
}
class A {
    fun q() {
        val y = 42
    }
}
class B {
    class C {
        fun u() {
            var z: Int = 15
        }
    }
}
