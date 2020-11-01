class A {/* NonLocalDeclarationFileStructureElement */
    class B {/* NonLocalDeclarationFileStructureElement */
        fun x() {/* IncrementallyReanalyzableFunction */
        }

        class C {/* NonLocalDeclarationFileStructureElement */

        }
    }

    class E {/* NonLocalDeclarationFileStructureElement */

    }

    fun y(): Int = 10/* IncrementallyReanalyzableFunction */
}