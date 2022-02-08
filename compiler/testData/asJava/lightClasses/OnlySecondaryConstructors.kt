// OnlySecondaryConstructors

class OnlySecondaryConstructors {
    constructor(): super()
    constructor(p: Int): this()
}

// LAZINESS:NoLaziness
// FIR_COMPARISON