// ISSUE: KT-71708

inline fun build(action: () -> Unit) {}

fun foo(x: Int) = build {
    if (x == 1) [1]
}
