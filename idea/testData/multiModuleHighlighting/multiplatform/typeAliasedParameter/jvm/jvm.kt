package foo

interface X
actual typealias Upper = X

actual interface BaseI {
    actual fun f(p: Upper)
}

internal class Impl : I, Base2 {
    override fun f(p: Upper) {
    }

    override fun foo(p: Upper) {}
}
