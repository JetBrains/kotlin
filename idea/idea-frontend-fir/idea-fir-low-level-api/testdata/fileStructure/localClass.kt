fun a() {/* ReanalyzableFunctionStructureElement */
    class X
}

class Y {/* NonReanalyzableDeclarationStructureElement */
    fun b() {/* ReanalyzableFunctionStructureElement */
        class Z
    }
}