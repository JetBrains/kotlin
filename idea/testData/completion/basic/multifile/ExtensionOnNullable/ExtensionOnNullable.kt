package first

fun firstFun() {
    val a : String? = ""
    a.hello<caret>
}

// EXIST: helloFun
// EXIST: helloFunPreventAutoInsert
// EXIST: helloWithParams
// ABSENT: helloFake
// NUMBER: 3