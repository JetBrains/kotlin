class A

fun f() {
    A.<error descr="[EXPRESSION_REQUIRED] ">fun g() {
        g()
    }</error>
}
