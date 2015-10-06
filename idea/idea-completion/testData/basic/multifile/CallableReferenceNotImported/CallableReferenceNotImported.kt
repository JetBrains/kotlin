fun String.foo() {
    val v = ::<caret>
}

// INVOCATION_COUNT: 2
// EXIST: topLevelFun
// EXIST: topLevelVal
// ABSENT: extFun
