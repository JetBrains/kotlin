abstract class Foo {/* ClassDeclarationStructureElement */
    abstract var id: Int/* DeclarationStructureElement */
        protected set
}

class Bar : Foo() {/* ClassDeclarationStructureElement */
    override var id: Int = 1/* DeclarationStructureElement */
    public set
}

fun test() {/* DeclarationStructureElement */
    val bar = Bar()
    bar.id = 1
}
