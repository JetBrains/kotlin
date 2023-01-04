fun x() {/* ReanalyzableFunctionStructureElement */
    fun y() {

    }
}

class A {/* NonReanalyzableClassDeclarationStructureElement */
    fun z() {/* ReanalyzableFunctionStructureElement */
        fun q() {

        }
    }
}
