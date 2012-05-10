class Some() {
    public val testPublic = 12
    protected val testProtected = 12
    private val testPrivate = 12
    val testPackage = 12
}

fun test() {
    Some().<caret>
}

// TIME: 2
// EXIST: testPublic, testProtected, testPrivate, testPackage