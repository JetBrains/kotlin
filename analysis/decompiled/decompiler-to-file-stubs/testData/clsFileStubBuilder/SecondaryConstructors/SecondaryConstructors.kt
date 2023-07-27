// FIR_IDENTICAL
package test

import java.io.Serializable

class SecondaryConstructors(x: Boolean) {
    init {
    }

    @anno constructor(x: String) : this(x == "abc") {
    }

    init {
    }

    private constructor(x: Int) : this(x < 0) {
    }

    inner class Inner<T : String, G : Int> where G : Serializable {
        constructor(x: T, g: G) {
        }
    }

    class Nested {
        @anno constructor(z: Int) {}
        internal constructor() {}
    }
}

annotation class anno