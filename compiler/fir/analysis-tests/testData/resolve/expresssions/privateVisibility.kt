// FILE: first.kt

private fun foo() {}

private class Private {
    private fun bar() {}

    fun baz() {
        bar()
        Nested()
        fromCompanion()
        NotCompanion.<!HIDDEN{LT}!><!HIDDEN{PSI}!>foo<!>()<!> // hidden
    }

    inner class Inner {
        fun foo() {
            bar()
            fromCompanion()
            NotCompanion.<!HIDDEN{LT}!><!HIDDEN{PSI}!>foo<!>()<!> // hidden
        }
    }

    private class Nested {
        fun foo() {
            fromCompanion()
            NotCompanion.<!HIDDEN{LT}!><!HIDDEN{PSI}!>foo<!>()<!> // hidden
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

    Local().<!HIDDEN{LT}!><!HIDDEN{PSI}!>bar<!>()<!> // hidden
}

fun test() {
    foo()
    Private().baz()
    Private().Inner()

    Private().<!HIDDEN{LT}!><!HIDDEN{PSI}!>bar<!>()<!> // hidden
    Private.<!HIDDEN{LT}!><!HIDDEN{PSI}!>Nested<!>()<!> // hidden
    Private.<!HIDDEN{LT}!><!HIDDEN{PSI}!>fromCompanion<!>()<!> // hidden
}

// FILE: second.kt

fun secondTest() {
    <!HIDDEN{LT}!><!HIDDEN{PSI}!>foo<!>()<!> // hidden
    <!HIDDEN{LT}!><!HIDDEN{PSI}!>Private<!>()<!> // hidden
}
