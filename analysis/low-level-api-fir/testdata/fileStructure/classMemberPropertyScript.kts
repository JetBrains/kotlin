/* NonReanalyzableNonClassDeclarationStructureElement */class X {
    var x: Int
        get() = field
        set(value) {
            field = value
        }

    val y = 42

    var z: Int = 15
}
