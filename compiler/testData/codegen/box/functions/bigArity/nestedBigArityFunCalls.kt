// JVM_ABI_K1_K2_DIFF: KT-63877
interface A
object O : A

typealias F<T> = (A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, T) -> String

fun test(f: F<F<String>>): String =
    f(O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O) {
            _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, s ->
        s
    }

fun box(): String {
    return test {
            _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, f ->
        f(O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, "OK")
    }
}