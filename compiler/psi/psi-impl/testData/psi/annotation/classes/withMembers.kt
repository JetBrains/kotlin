// A nested classifier is the only declaration an annotation class may have besides its parameters
package test

annotation class Outer(val value: Int) {
    annotation class Nested(val inner: String)

    companion object {
        const val DEFAULT: Int = 1
    }
}
