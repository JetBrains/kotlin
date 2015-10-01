fun String.foo() {
    bar(::<caret>)
}

fun bar(p: () -> Unit) { }
fun bar(p: String.() -> Unit) { }


// INVOCATION_COUNT: 2
// EXIST: topLevelFun
// ABSENT: extFun
