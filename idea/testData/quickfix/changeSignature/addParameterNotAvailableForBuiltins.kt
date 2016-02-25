// "class org.jetbrains.kotlin.idea.quickfix.AddFunctionParametersFix" "false"
// ERROR: Too many arguments for public open operator fun equals(other: Any?): Boolean defined in kotlin.Any

fun f(d: Any) {
    d.equals("a", <caret>"b")
}
