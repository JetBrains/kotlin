// "Replace with 'newFun()'" "true"

@Deprecated("", ReplaceWith("newFun()"))
@DeprecatedSinceKotlin(warningSince = "1.0")
fun oldFun() {
    newFun()
}

fun newFun() {}

fun foo() {
    <caret>oldFun()
}
