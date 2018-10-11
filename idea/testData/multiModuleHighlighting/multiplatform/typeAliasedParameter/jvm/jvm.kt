package foo

interface X
actual typealias Upper = X

actual interface BaseI {
    actual fun f(p: Upper)
}

internal class Impl : I {
    override fun f(p: Upper) {
    }
}