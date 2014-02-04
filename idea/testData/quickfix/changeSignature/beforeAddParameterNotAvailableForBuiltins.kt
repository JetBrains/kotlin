// "class org.jetbrains.jet.plugin.quickfix.AddFunctionParametersFix" "false"
// ERROR: Too many arguments for public open fun equals(other: jet.Any?): jet.Boolean defined in jet.Any

fun f(d: Any) {
    d.equals("a", <caret>"b")
}