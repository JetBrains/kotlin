// @ParameterName annotation is ignored when not used in function type
fun call(a: @ParameterName("notMe") Int, b: @ParameterName("meNeither") String) {
    <expr>call(1, "")</expr>
}
