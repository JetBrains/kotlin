// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR

@OptIn(ExperimentalStdlibApi::class)
fun foo1() {
    buildList {
        object {
            fun foo() = add("")
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun foo2() {
    buildList {
        class A {
            fun foo() = add("")
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun foo3() {
    buildList {
        object {
            var x: Int
                get() = 1
                set(value) {
                    add(value)
                }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun foo4() {
    buildList {
        class A {
            var x: Int
                get() = 1
                set(value) {
                    add(value)
                }
        }
    }
}

fun box(): String {
    foo1()
    foo2()
    foo3()
    foo4()
    return "OK"
}
