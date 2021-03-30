// FIR_COMPARISON
package first

interface TestedTrait() {
}

fun firstFun() {
    val a = second.SomeTest<TestedTrait>()
    a.testing<caret>
}

// EXIST: testingMethod
// EXIST: testingExpectedFunction
// ABSENT: testingUnexpectedFunction

// NOTHING_ELSE
