@Suppress("DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE")
@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.0")
fun f(x: Int) {}

fun d(x: Int) {
    f(<caret>1)
}
/*
Text: (<highlight>x: Int</highlight>), Disabled: false, Strikeout: true, Green: true
*/