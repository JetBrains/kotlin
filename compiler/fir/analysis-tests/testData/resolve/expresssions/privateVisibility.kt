// FILE: first.kt

private fun foo() {}

private class Private {
    private fun bar() {}

    fun baz() {
        bar()
        Nested()
        fromCompanion()
        NotCompanion.<!HIDDEN!>foo<!>() // hidden
    }

    inner class Inner {
        fun foo() {
            bar()
            fromCompanion()
            NotCompanion.<!HIDDEN!>foo<!>() // hidden
        }
    }

    private class Nested {
        fun foo() {
            fromCompanion()
            NotCompanion.<!HIDDEN!>foo<!>() // hidden
        }
    }

    companion object {
        private fun fromCompanion() {}
    }

    object NotCompanion {
        private fun foo() {}
    }
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

    Local().<!HIDDEN!>bar<!>() // hidden
}

fun test() {
    foo()
    Private().baz()
    Private().Inner()

    Private().<!HIDDEN!>bar<!>() // hidden
    Private.<!HIDDEN!>Nested<!>() // hidden
    Private.<!HIDDEN!>fromCompanion<!>() // hidden
}

// FILE: second.kt

fun secondTest() {
    <!HIDDEN!>foo<!>() // hidden
    <!HIDDEN!>Private<!>() // hidden
}
