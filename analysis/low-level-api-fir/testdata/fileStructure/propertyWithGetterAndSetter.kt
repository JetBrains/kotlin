var withGetterAndSetter: Int = 42/* ReanalyzablePropertyStructureElement */
    get() = field
    set(value) {
        field = value
    }
