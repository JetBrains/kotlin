/* RootScriptStructureElement */fun a() {/* ReanalyzableFunctionStructureElement */
    class X
}

class Y {/* NonReanalyzableClassDeclarationStructureElement */
    fun b() {/* ReanalyzableFunctionStructureElement */
        class Z
    }
}
