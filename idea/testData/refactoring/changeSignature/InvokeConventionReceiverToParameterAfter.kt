class A

fun invoke(a: A, s: String, i: Int) {}

fun usage(a: A) {
    invoke(a, "", 42)
    invoke(A(), "", 42)
    invoke(a, "", 42)
}