// FIR_IDENTICAL

class A

fun f() {
    A.<error>class B {
        fun f() {
            f()
        }
    }</error>
}
