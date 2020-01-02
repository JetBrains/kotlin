class Outer {
    val outerProp: String
    inner class Inner(inner: Inner, outer: Outer) {
        val innerProp: String
        init {
            outerProp // use of outerProp is ok because we're suppose that Outer instance should be initialized
            this@Outer.outerProp

            this@Outer.outerProp = "1"
            outerProp = "2" // do not repeat the same diagnostic with this receiver of outer class
            outer.outerProp = "3"

            innerProp = "4" + inner.innerProp
            this@Inner.innerProp = "5"
            innerProp = "6" // do not repeat the same diagnostic with this receiver
            this@Inner.innerProp = "7"

            inner.innerProp = "8"
        }
    }
}
