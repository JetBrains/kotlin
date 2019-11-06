// FILE: first.kt

private fun foo() {}

private class Private {
    private fun bar() {}

    fun baz() {
        bar()
        Nested()
    }

    inner class Inner {
        fun foo() {
            bar()
        }
    }

    private class Nested
}

fun withLocals() {
    class Local {
        private fun bar()

        fun baz() {
            bar()
            Inner()
        }

        private inner class Inner {
            fun foo() {
                bar()
            }
        }
    }

    Local().baz()

    Local().<!INAPPLICABLE_CANDIDATE!>bar<!>() // hidden
}

fun test() {
    foo()
    Private().baz()
    Private().Inner()

    Private().<!INAPPLICABLE_CANDIDATE!>bar<!>() // hidden
    Private.<!INAPPLICABLE_CANDIDATE!>Nested<!>() // hidden
}

// FILE: second.kt

fun secondTest() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>() // hidden
    <!INAPPLICABLE_CANDIDATE!>Private<!>() // hidden
}