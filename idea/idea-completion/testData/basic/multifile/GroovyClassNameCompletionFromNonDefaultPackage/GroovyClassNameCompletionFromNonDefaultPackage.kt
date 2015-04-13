package foo

class TestKotlin

val test = Test<caret>

// EXIST: TestKotlin
// EXIST: TestGroovyNormal
// EXIST: TestGroovyScript
// INVOCATION_COUNT: 2
