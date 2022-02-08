// FILE: first.kt

private fun foo() {}

private class Private {
    private fun bar() {}

    fun baz() {
        bar()
        Nested()
        fromCompanion()
        NotCompanion.<!INVISIBLE_REFERENCE!>foo<!>() // hidden
    }

    inner class Inner {
        fun foo() {
            bar()
            fromCompanion()
            NotCompanion.<!INVISIBLE_REFERENCE!>foo<!>() // hidden
        }
    }

    private class Nested {
        fun foo() {
            fromCompanion()
            NotCompanion.<!INVISIBLE_REFERENCE!>foo<!>() // hidden
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
        <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>private fun bar()<!>

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

    Local().<!INVISIBLE_REFERENCE!>bar<!>() // hidden
}

fun test() {
    foo()
    Private().baz()
    Private().Inner()

    Private().<!INVISIBLE_REFERENCE!>bar<!>() // hidden
    Private.<!INVISIBLE_REFERENCE!>Nested<!>() // hidden
    Private.<!INVISIBLE_REFERENCE!>fromCompanion<!>() // hidden
}

// FILE: second.kt

fun secondTest() {
    <!INVISIBLE_REFERENCE!>foo<!>() // hidden
    <!INVISIBLE_REFERENCE!>Private<!>() // hidden
}
