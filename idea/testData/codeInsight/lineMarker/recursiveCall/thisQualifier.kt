class Outer {
    fun f(a: Int) {
    }

    class F {
        fun f(a: Int) {
            if (a > 0) {
                this.<lineMarker>f</lineMarker>(a - 1)
                this@F.<lineMarker>f</lineMarker>(a - 1)
                ((this@F)).<lineMarker>f</lineMarker>(a - 1)

                this@Outer.f(a - 1)
            }
        }
    }
}
