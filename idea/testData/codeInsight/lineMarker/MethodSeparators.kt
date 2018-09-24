// METHOD_SEPARATORS

class Foo {
    object BarObj {
    }

    fun bar() {

    }

    <lineMarker descr="null">fun</lineMarker> baz() {
        class FooLocal {
        }

        fun fooLocal() {
        }

        <lineMarker descr="null">fun</lineMarker> barLocal() {
        }

        val xLocal = run {
            "x"
        }
    }

    <lineMarker descr="null">val</lineMarker> x = 0

    <lineMarker descr="null">val</lineMarker> y: Int
        get() = 0

    <lineMarker descr="null">val</lineMarker> z = run {
        "abc"
    }

    <lineMarker descr="null">fun</lineMarker> quux() {}
    fun xyzzy() {}
}
