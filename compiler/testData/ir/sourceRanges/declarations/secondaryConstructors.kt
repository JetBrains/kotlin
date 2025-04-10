class C1 {
    constructor() : super()

    private
    constructor(x: Int): super()

    /**
     * comment
     */
    constructor(x: String) : super()

    @Suppress("UNUSED_VARIABLE")
    constructor(x: Any): super()
}

class C2 {
    constructor()

    @Suppress("UNUSED_VARIABLE")
    constructor(p: String = "") : this()
}
