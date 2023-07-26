abstract class Foo {/* NonReanalyzableClassDeclarationStructureElement */
    abstract var id: Int/* ReanalyzablePropertyStructureElement */
        protected set
}

class Bar : Foo() {/* NonReanalyzableClassDeclarationStructureElement */
    override var id: Int = 1/* ReanalyzablePropertyStructureElement */
    public set
}

fun test() {/* ReanalyzableFunctionStructureElement */
    val bar = Bar()
    bar.id = 1
}
