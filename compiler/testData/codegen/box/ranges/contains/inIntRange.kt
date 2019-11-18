// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun box(): String {
    for (x in 1..10) {
        assert(x in 1..10)
        assert(x + 10 !in 1..10)
    }

    var x = 0
    assert(0 !in 1..2)

    assert(++x in 1..1)
    assert(++x !in 1..1)

    assert(sideEffect(x) in 2..3)
    return "OK"
}


var invocationCounter = 0
fun sideEffect(x: Int): Int {
    ++invocationCounter
    assert(invocationCounter == 1)
    return x
}