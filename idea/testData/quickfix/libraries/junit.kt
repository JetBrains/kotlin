// "Add 'JUnit4' to classpath" "true"
// ERROR: Unresolved reference: Before
// ERROR: Unresolved reference: junit
// UNCONFIGURE_LIBRARY: JUnit4
package some

import org.<caret>junit.Before

open class KBase {
    @Before
    fun setUp() {
        throw UnsupportedOperationException()
    }
}