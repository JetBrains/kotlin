package first

fun firstFun() {
    val a = SomeUnknownClass()
    a.hello<caret>
}

// ABSENT: helloFun
// ABSENT: helloFunPreventAutoInsert
// ABSENT: helloWithParams
// INVOCATION_COUNT: 0