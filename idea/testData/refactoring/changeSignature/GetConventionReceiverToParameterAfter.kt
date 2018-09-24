class A

fun get(a: A, s: String, i: Int) {}

fun usage(a: A) {
    get(a, "", 42)
    get(A(), "", 42)
    get(a, "", 42)
}