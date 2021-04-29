/*
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37431
 */

class Case1() {

    fun foo() {
        val x = sequence<String> {

            val  y = this
            //this is Case1 instead of SequenceScope<String>
            yield("") // UNRESOLVED_REFERENCE

            this.yield("") //UNRESOLVED_REFERENCE

            this <!USELESS_CAST!>as SequenceScope<String><!>

            yield("") // resolved to SequenceScope.yield

            this.yield("") // resolved to SequenceScope.yield
        }
    }
}

fun case2() {
    val x = sequence<String> {

        val  y = this
        yield("") // UNRESOLVED_REFERENCE

        this.yield("") //UNRESOLVED_REFERENCE

        this <!USELESS_CAST!>as SequenceScope<String><!>

        yield("") // UNRESOLVED_REFERENCE

        this.yield("") // UNRESOLVED_REFERENCE
    }
}
