package test

class SecondaryConstructors(x: Boolean) {
    init {
    }

    anno constructor(x: String) : this(x == "abc") {
    }

    init {
    }

    private constructor(x: Int) : this(x < 0) {
    }

    inner class Inner<T : String, G : Int> where G : Number {
        constructor(x: T, g: G) {
        }
    }

    class Nested {
        anno constructor(z: Int) {}
        internal constructor() {}
    }
}

annotation class anno