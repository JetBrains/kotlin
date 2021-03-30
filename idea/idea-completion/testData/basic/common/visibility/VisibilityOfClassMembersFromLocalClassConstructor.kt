// FIR_COMPARISON
package test

open class Base {
    public val testPublic = 12
    protected val testProtected = 12
    private val testPrivate = 12
    val testPackage = 12

    fun baseClassMember() {
        class Local {
            init {
                <caret>
            }
        }
    }
}

// EXIST: testPublic, testProtected, testPackage, testPrivate
