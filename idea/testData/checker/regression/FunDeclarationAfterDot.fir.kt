class A

fun f() {
    A.<error descr="[ANONYMOUS_FUNCTION_WITH_NAME] ">fun g() {
        g()
    }</error>
}
