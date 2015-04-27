trait <caret>A

trait B: A

class MyClass(a: A = run { trait X: A; object: A {} }) {
    inner trait G: A {}

    init {
        trait C: A
    }

    fun foo(a: A = run { trait X: A; object: A {} }) {
        trait D: A

        val t = object {
            inner trait F: A
        }
    }
}

val bar: Int
    get() {
        trait E: A

        return 0
    }