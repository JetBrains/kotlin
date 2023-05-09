class A {/* NonReanalyzableClassDeclarationStructureElement */
    class B {/* NonReanalyzableClassDeclarationStructureElement */
        fun x() {/* ReanalyzableFunctionStructureElement */
        }

        class C {/* NonReanalyzableClassDeclarationStructureElement */

        }
    }

    class E {/* NonReanalyzableClassDeclarationStructureElement */

    }

    fun y(): Int = 10/* ReanalyzableFunctionStructureElement */
}
