class Owner<T>(x: Int) {
    constructor(t: T, y: Int) : this(y + 1) {
        class LocalInConstructor(val t: T)
        LocalInConstructor(t)
    }

    init {
        class LocalInInitializer(val t: T)
    }

    fun foo(t: T) {
        class LocalInFunction(val t: T)
        LocalInFunction(t)
    }
}
