/* NonReanalyzableNonClassDeclarationStructureElement */var x: Int = 10
    get() = field
    set(value) {
        field = value
    }

class X {
    var y: Int = 10
        get() = field
        set(value) {
            field = value
        }
}
