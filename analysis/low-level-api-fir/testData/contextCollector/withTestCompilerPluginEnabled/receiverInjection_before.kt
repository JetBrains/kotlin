// WITH_FIR_TEST_COMPILER_PLUGIN

interface Algebra<T> {
    operator fun T.plus(other: T): T
}

interface A
interface B

fun <T> injectAlgebra() {}

fun test_1(a1: A, a2: A) {
    <expr>a1 + a2</expr>

    injectAlgebra<A>()
}