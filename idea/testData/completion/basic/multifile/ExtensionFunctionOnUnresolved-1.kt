package first

fun firstFun() {
    val a = SomeUnknownClass()
    a.hello<caret>
}

// ABSENT: helloFun
// ABSENT: helloFunPreventAutoInsert
// ABSENT: helloWithParams
// NUMBER: 0