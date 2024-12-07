fun foo() {/* DeclarationStructureElement */
    var x: Int
}
class A {/* ClassDeclarationStructureElement */
    fun q() {/* DeclarationStructureElement */
        val y = 42
    }
}
class B {/* ClassDeclarationStructureElement */
    class C {/* ClassDeclarationStructureElement */
        fun u() {/* DeclarationStructureElement */
            var z: Int = 15
        }
    }
}
