class A {/* NonReanalyzableDeclarationStructureElement */
    class B {/* NonReanalyzableDeclarationStructureElement */
        fun x() {/* ReanalyzableFunctionStructureElement */
        }

        class C {/* NonReanalyzableDeclarationStructureElement */

        }
    }

    class E {/* NonReanalyzableDeclarationStructureElement */

    }

    fun y(): Int = 10/* ReanalyzableFunctionStructureElement */
}