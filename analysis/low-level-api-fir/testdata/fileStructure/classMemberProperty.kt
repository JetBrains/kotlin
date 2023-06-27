class X {/* NonReanalyzableClassDeclarationStructureElement */
    var x: Int/* ReanalyzablePropertyStructureElement */
        get() = field
        set(value) {
            field = value
        }

    val y = 42/* NonReanalyzableNonClassDeclarationStructureElement */

    var z: Int = 15/* NonReanalyzableNonClassDeclarationStructureElement */
}
