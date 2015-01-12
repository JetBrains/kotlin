// "class org.jetbrains.kotlin.idea.quickfix.AddFunctionParametersFix" "false"
// ERROR: Too many arguments for public open fun equals(other: kotlin.Any?): kotlin.Boolean defined in kotlin.Any

fun f(d: Any) {
    d.equals("a", <caret>"b")
}
