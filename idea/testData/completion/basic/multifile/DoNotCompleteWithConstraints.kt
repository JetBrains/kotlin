package first

trait TestedTrait() {
}

fun firstFun() {
    val a = second.SomeTest<TestedTrait>()
    a.testing<caret>
}

// EXIST: testingMethod
// EXIST: testingExpectedFunction
// ABSENT: testingUnexpectedFunction

// NUMBER: 2