// @ParameterName annotation takes precedence over name in function type parameter
fun call(x: (notMe: @ParameterName("a") Int, meNeither: @ParameterName("b") String) -> Unit) {
    <expr>x(1, "")</expr>
}
