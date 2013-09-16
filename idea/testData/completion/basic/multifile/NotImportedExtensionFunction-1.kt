package first

fun firstFun() {
    val a = ""
    a.hello<caret>
}

// EXIST: helloFun
// EXIST: helloFunPreventAutoInsert
// EXIST: helloWithParams
// ABSENT: helloFake
// INVOCATION_COUNT: 3