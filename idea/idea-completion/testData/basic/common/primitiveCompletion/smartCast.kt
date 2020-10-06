// FIR_COMPARISON
interface A {
    fun a()
}

interface B {
    fun b()
}

interface C {
    fun c()
}

fun take(a: A) {
    if (a is B && a is C) {
        a.<caret>
    }
}

// EXIST: a
// EXIST: b
// EXIST: c