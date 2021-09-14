var x: Int = 10/* ReanalyzablePropertyStructureElement */
    get() = field
    set(value) {
        field = value
    }

class X {/* NonReanalyzableDeclarationStructureElement */
    var y: Int = 10/* ReanalyzablePropertyStructureElement */
        get() = field
        set(value) {
            field = value
        }
}