fun foo() {/* ReanalyzableFunctionStructureElement */
    var x: Int
}
class A {/* NonReanalyzableDeclarationStructureElement */
    fun q() {/* ReanalyzableFunctionStructureElement */
        val y = 42
    }
}
class B {/* NonReanalyzableDeclarationStructureElement */
    class C {/* NonReanalyzableDeclarationStructureElement */
        fun u() {/* ReanalyzableFunctionStructureElement */
            var z: Int = 15
        }
    }
}
