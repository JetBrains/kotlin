package first

fun firstFun() {
    val a = ""
    a.hello<caret>
}

// EXIST: helloFun
// EXIST: helloFunPreventAutoInsert
// NUMBER: 2