import publicMutableField as A

fun box(): String {
    val a = A()
    val f = A::field
    if (f.get(a) != 239) return "Fail 1: ${f.get(a)}"
    f[a] = 42
    if (f.get(a) != 42) return "Fail 2: ${f.get(a)}"
    if (f.get(a) != 42) return "Fail 2: ${f.get(a)}"
    return "OK"
}
