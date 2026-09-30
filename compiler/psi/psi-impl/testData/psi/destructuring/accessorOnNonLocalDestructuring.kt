// COMPILATION_ERRORS
// IGNORE_ERRORS_FROM_API: KT-74793
val (a, b) = 1 to 2
    get

val [c, d] = 1 to 2
    get() = 1
    set(value) {}

val (e, f) = 1 to 2
    field = 3

val (g, h) = 1 to 2; get

val (i, j) = 1 to 2;
val k = 1

class A {
    val (l, m) = 1 to 2
        get

    var (n, o) = 1 to 2
        private set
}
