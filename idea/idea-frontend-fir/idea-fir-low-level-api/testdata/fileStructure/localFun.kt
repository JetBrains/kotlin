fun x() {/* ReanalyzableFunctionStructureElement */
    fun y() {

    }
}

class A {/* NonReanalyzableDeclarationStructureElement */
    fun z() {/* ReanalyzableFunctionStructureElement */
        fun q() {

        }
    }
}