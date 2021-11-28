var x: Int/* ReanalyzablePropertyStructureElement */
    get() = field
    set(value) {
        field = value
    }

val y = 42/* NonReanalyzableDeclarationStructureElement */

var z: Int = 15/* ReanalyzablePropertyStructureElement */