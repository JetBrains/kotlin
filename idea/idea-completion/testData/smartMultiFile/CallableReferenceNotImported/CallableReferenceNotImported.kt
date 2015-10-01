fun String.foo() {
    bar(::<caret>)
}

fun bar(p: () -> Unit) { }
fun bar(p: String.() -> Unit) { }


// INVOCATION_COUNT: 2
// EXIST: extFun
// EXIST: topLevelFun
// ABSENT: wrongExtFun
// ABSENT: wrongTopLevelFun
