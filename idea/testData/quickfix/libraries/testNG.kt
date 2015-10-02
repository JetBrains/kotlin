// "Add 'testng' to classpath" "true"
// ERROR: Unresolved reference: BeforeMethod
// ERROR: Unresolved reference: testng
// UNCONFIGURE_LIBRARY: testng
package some

abstract class KBase {
    @<caret>BeforeMethod
    fun setUp() {
        throw UnsupportedOperationException()
    }
}