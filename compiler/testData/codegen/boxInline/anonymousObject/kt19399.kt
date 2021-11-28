// WITH_STDLIB
// FILE: 1.kt
class Foo {
    var bar = ""

    inline fun ifNotBusyPerform(action: (complete: () -> Unit) -> Unit) {
        action {
            bar += "K"
        }
    }

    fun ifNotBusySayHello() {
        ifNotBusyPerform {
            bar += "O"
            it()
        }
    }

    inline fun inlineFun(s: () -> Unit) {
        s()
    }

    fun start() {
        inlineFun {
            {
                ifNotBusyPerform {
                    ifNotBusySayHello()
                }
            }()
        }
    }
}

// FILE: 2.kt

fun box(): String {
    val foo = Foo()
    foo.start()

    return foo.bar
}
