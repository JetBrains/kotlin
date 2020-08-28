// FIR_COMPARISON
interface A {
    fun memberA()
}

interface B {
    fun memberB()
}

interface C {
    fun memberC()
}

fun C.test(a: Any) {
    if (a is A && a is B) {
        a.<caret>
    }
}

// EXIST: memberA
// EXIST: memberB
// ABSENT: memberC
