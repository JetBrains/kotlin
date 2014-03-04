// "class org.jetbrains.jet.plugin.quickfix.AddFunctionParametersFix" "false"
// ERROR: Too many arguments for public open fun equals(other: kotlin.Any?): kotlin.Boolean defined in kotlin.Any

fun f(d: Any) {
    d.equals("a", <caret>"b")
}