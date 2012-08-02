package a

class A<T>(x: T) {
    val p = x
}

fun <T, G> A<T>.foo(x: (T)-> G): G {
    return x(this.p)
}

fun main(args: Array<String>) {
    val a = A(1)
    val t: String = a.<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>foo<!>({p -> p})
    t : String
}