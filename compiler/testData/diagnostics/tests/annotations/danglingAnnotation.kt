// FIR_IDENTICAL
annotation class Anno(val position: String)

interface OriginalInterface {
    companion object {
        private const val prop = 0
    }

    @Anno("dangling $prop")<!SYNTAX!><!>
}
