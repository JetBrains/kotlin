fun bar(a: Int, b: (Boolean) -> Unit) {/* ReanalyzableFunctionStructureElement */

}

class A {/* NonReanalyzableClassDeclarationStructureElement */
    fun foo(x: String, y: () -> String) {/* ReanalyzableFunctionStructureElement */

    }
}
