class Some() {
    public val testPublic = 12
    protected val testProtected = 12
    private val testPrivate = 12
    val testPackage = 12
}

class SomeSubclass : Some() {
    fun test() {
        <caret>
    }
}

// TIME: 2
// EXIST: testPublic, testProtected, testPackage

// Should exist after KT-1805 Better diagnostic for access to private field of parent class
// ABSENT: testPrivate