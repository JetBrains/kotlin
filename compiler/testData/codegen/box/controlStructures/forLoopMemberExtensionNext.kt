// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class It {
    var hasNext = true
    operator fun hasNext() = if (hasNext) {hasNext = false; true} else false
}

class C {
    operator fun iterator(): It = It()
}

class X {
    operator fun It.next() = 5

    fun test() {
        for (i in C()) {
            foo(i)
        }
    }

}

fun foo(x: Int) {}

fun box(): String {
    X().test()
    return "OK"
}
