package first

fun firstFun() {
    val a = ""
    a.hello<caret>
}

// EXIST: helloFun
// EXIST: helloFunPreventAutoInsert
// EXIST: helloWithParams
// NUMBER: 3