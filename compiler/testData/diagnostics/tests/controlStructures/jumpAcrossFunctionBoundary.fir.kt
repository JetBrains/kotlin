fun call(f: () -> Unit) = f()

fun f1() {
    outer@ while (true) {
        call {
            break@outer
        }
    }
}

fun f2() {
    do {
        fun inner() {
            continue
        }
    } while (true)
}
