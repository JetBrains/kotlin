// Constructors

class Constructors(val valInPrimary: Int) {
    constructor(parameterInSecondary: String): this(4)
    private constructor(): this(2)
}

// LAZINESS:NoLaziness
// FIR_COMPARISON