// "Add 'operator' modifier" "true"
// ERROR: 'operator' modifier is required on 'component2' in 'A'

class A {
    fun component1(): Int = 0
    fun component2(): Int = 1
}

fun foo() {
    val (<caret>zero, one) = A()
}


/* IGNORE_FIR */