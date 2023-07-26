class A(val x: Int = 10, val b: String)/* NonReanalyzableNonClassDeclarationStructureElement */ {/* NonReanalyzableClassDeclarationStructureElement */
    constructor(i: Int) : this(x = 1, b = i.toString())/* NonReanalyzableNonClassDeclarationStructureElement */
}
