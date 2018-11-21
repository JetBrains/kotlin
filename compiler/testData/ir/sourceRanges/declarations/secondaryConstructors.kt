class C {
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