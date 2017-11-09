operator fun Any.plusAssign(lambda: () -> Unit) {}

operator fun Any.get(index: () -> Unit): Int = 42
operator fun Any.set(index: () -> Unit, value: Int) {}

fun test1(a: Any) {
    a += {  }
}

fun test2(a: Any) {
    a[{}] += 42
}

fun test3(a: Any) {
    a[{}]++
}