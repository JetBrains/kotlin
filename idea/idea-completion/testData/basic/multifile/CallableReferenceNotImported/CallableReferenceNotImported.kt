fun String.foo() {
    val v = ::<caret>
}

// INVOCATION_COUNT: 2
// EXIST: extFun
// EXIST: topLevelFun
// EXIST: topLevelVal
// ABSENT: wrongExtFun
