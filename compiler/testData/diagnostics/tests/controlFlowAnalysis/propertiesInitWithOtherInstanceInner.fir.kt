class Outer {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val outerProp: String<!>
    inner class Inner(inner: Inner, outer: Outer) {
        val innerProp: String
        init {
            outerProp // use of outerProp is ok because we're suppose that Outer instance should be initialized
            this@Outer.outerProp

            this@Outer.<!VAL_REASSIGNMENT!>outerProp<!> = "1"
            <!VAL_REASSIGNMENT!>outerProp<!> = "2" // do not repeat the same diagnostic with this receiver of outer class
            outer.<!VAL_REASSIGNMENT!>outerProp<!> = "3"

            innerProp = "4" + inner.innerProp
            this@Inner.<!VAL_REASSIGNMENT!>innerProp<!> = "5"
            <!VAL_REASSIGNMENT!>innerProp<!> = "6" // do not repeat the same diagnostic with this receiver
            this@Inner.<!VAL_REASSIGNMENT!>innerProp<!> = "7"

            inner.<!VAL_REASSIGNMENT!>innerProp<!> = "8"
        }
    }
}
