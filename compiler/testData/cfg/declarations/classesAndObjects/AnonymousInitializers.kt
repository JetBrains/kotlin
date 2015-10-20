class AnonymousInitializers() {
    val k = 34

    val i: Int
    init {
        i = 12
    }

    val j: Int
       get() = 20

    init {
        i = 13
    }
}