// OnlySecondaryConstructors

class OnlySecondaryConstructors {
    constructor(): super()
    constructor(p: Int): this()
}
