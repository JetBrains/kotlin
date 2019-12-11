fun test() {
    while (true) {
        fun local1() {
            break
        }
    }
}

fun test2() {
    while (true) {
        {
            continue
        }
    }
}

fun test3() {
    while (true) {
        class LocalClass {
            init {
                continue
            }

            fun foo() {
                break
            }
        }
    }
}

fun test4() {
    while (true) {
        object: Any() {
            init {
                break
            }
        }
    }
}

fun test5() {
    while (true) {
        class LocalClass(val x: Int) {
            constructor() : this(42) {
                break
            }
            constructor(y: Double) : this(y.toInt()) {
                continue
            }
        }
    }
}

fun test6() {
    while (true) {
        class LocalClass(val x: Int) {
            init {
                break
            }
            init {
                continue
            }
        }
    }
}

fun test7() {
    while (true) {
        class LocalClass {
            val x: Int = if (true) {
                break
            }
            else {
                continue
            }
        }
    }
}

fun test8() {
    while (true) {
        class LocalClass(val x: Int) {
            constructor() : this(if (true) { 42 } else { break })
        }
    }
}