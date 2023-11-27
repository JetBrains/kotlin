annotation class Anno(val position: String)

interface OriginalInterface {
    companion object {
        private const val prop = 0
    }

    @Anno("dangling $<!UNRESOLVED_REFERENCE!>prop<!>")<!SYNTAX!><!>
}
