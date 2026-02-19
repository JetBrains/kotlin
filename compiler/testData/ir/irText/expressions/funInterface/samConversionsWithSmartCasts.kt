fun interface KRunnable {
    fun run()
}

fun <T> id(x: T) = x

fun run1(r: KRunnable) {}
fun run2(r1: KRunnable, r2: KRunnable) {}

fun <T> test0(a: T) where T : KRunnable, T : () -> Unit {
    run1(a)
}

fun test1(a: () -> Unit) {
    if (a is KRunnable) {
        run1(a)
    }
}

fun test2(a: KRunnable) {
    a as () -> Unit
    run1(a)
}

fun test3(a: () -> Unit) {
    if (a is KRunnable) {
        run2(a, a)
    }
}

fun test4(a: () -> Unit, b: () -> Unit) {
    if (a is KRunnable) {
        run2(a, b)
    }
}

fun test5(a: Any) {
    if (a is KRunnable) {
        run1(a)
    }
}

fun test5x(a: Any) {
    if (a is KRunnable) {
        a as () -> Unit
        run1(a)
    }
}

fun test6(a: Any) {
    a as () -> Unit
    run1(a)
}

fun test7(a: (Int) -> Int) {
    a as () -> Unit
    run1(a)
}

fun <T : (Int) -> Int> test7a(a: T) {
    a as () -> Unit
    run1(a)
}

fun <T> test7b(a: T) where T : (Int) -> Unit, T : () -> Unit {
    run1(a)
}

interface Unrelated

fun <T> test7c(a: T) where T : Unrelated, T : () -> Unit {
    run1(a)
}

fun test8(a: () -> Unit) {
    run1(id(a))
}

fun test9() {
    run1(::test9)
}

// KT-63345
fun test10(a: Any) {
    @Suppress("CANNOT_CHECK_FOR_ERASED")
    if (a is Unrelated && a is (() -> Unit)) {
        run1(a)
    }
}
