// OnlySecondaryConstructors

class OnlySecondaryConstructors {
    constructor(): super()
    constructor(p: Int): this()
}

// FIR_COMPARISON