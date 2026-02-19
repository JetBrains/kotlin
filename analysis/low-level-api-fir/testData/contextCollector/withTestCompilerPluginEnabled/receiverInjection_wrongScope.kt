// WITH_FIR_TEST_COMPILER_PLUGIN

interface Algebra<T> {
    operator fun T.plus(other: T): T
}

interface A
interface B

fun <T> injectAlgebra() {}

fun test_1(flag: Boolean, a1: A, a2: A) {
    if (flag) {
        injectAlgebra<A>()
    }

    run {
        injectAlgebra<A>()
    }

    <expr>a1 + a2</expr>
}