annotation class Anno(val position: String)

fun foo() {
    class OriginalClass {
        val prop = 0

        @Anno("dangling $<!UNRESOLVED_REFERENCE!>prop<!>")<!SYNTAX!><!>
    }
}
