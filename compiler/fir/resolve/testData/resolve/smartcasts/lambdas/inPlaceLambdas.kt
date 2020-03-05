// !DUMP_CFG
interface A {
    fun foo()
}

interface B {
    fun bar()
}


inline fun run(block: () -> Unit) {
    block()
}

fun test_1(x: Any?) {
    if (x is A) {
        run {
            x.foo()
        }
    }
}

fun test_2(x: Any?) {
    run {
        x as B
    }
    x.bar()
}

fun test_3(x: Any?) {
    if (x is A) {
        run {
            x.foo()
            x as B
        }
        x.bar()
    }
}