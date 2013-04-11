class Some() {
    public val testPublic : Int = 12
    protected val testProtected : Int = 12
    private val testPrivate = 12
    val testPackage = 12
}

fun test() {
    Some().<caret>
}

// TIME: 2
// EXIST: testPublic, testProtected, testPrivate, testPackage