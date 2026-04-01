// COMPILATION_ERRORS

fun test1() {
    @Ann (var first) = x
    @Ann(var first) = x
    @Ann() (var first) = x
    @Ann("") (var first) = x
    @Ann(val first) = x
    @Ann() (val first) = x
    @Ann("") (val first) = x
}