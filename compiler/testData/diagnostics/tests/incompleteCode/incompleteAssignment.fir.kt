// COMPARE_WITH_LIGHT_TREE

package sum

import java.util.*
fun sum(a : IntArray) : Int {
    // Write your solution here
    <!UNRESOLVED_REFERENCE!>res<!> = 0
    for (e in a)
        <!UNRESOLVED_REFERENCE!>res<!> +=<!SYNTAX!><!>
}
fun main() {
    test(0)
    test(1, 1)
    test(-1, -1, 0)
    test(6, 1, 2, 3)
    test(6, 1, 1, 1, 1, 1, 1)
}
// HELPER FUNCTIONS
fun test(expectedSum : Int, vararg data : Int) {
    val actualSum = sum(data)
    assertEquals(actualSum, expectedSum, "\ndata = ${Arrays.toString(data)}\n" +
    "sum(data) = ${actualSum}, but must be $expectedSum ")
}
fun <T: Any> assertEquals(actual : T?, expected : T?, message : Any? = null) {
    if (actual != expected) {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
