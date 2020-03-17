/*
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37431
 */

class Case1() {

    fun foo() {
        val x = sequence<String> {

            val  y = this
            //this is Case1 instead of SequenceScope<String>
            <!UNRESOLVED_REFERENCE!>yield<!>("") // UNRESOLVED_REFERENCE

            this.<!UNRESOLVED_REFERENCE!>yield<!>("") //UNRESOLVED_REFERENCE

            this as SequenceScope<String>

            yield("") // resolved to SequenceScope.yield

            this.yield("") // resolved to SequenceScope.yield
        }
    }
}

fun case2() {
    val x = sequence<String> {

        val  y = this
        <!UNRESOLVED_REFERENCE!>yield<!>("") // UNRESOLVED_REFERENCE

        this.<!UNRESOLVED_REFERENCE!>yield<!>("") //UNRESOLVED_REFERENCE

        this as SequenceScope<String>

        <!UNRESOLVED_REFERENCE!>yield<!>("") // UNRESOLVED_REFERENCE

        this.<!UNRESOLVED_REFERENCE!>yield<!>("") // UNRESOLVED_REFERENCE
    }
}