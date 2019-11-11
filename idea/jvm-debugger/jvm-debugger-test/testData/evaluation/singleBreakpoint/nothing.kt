package nothing

fun main() {
    //Breakpoint!
    val a = 5
}

fun block(foo: () -> Unit) {
    foo()
}

// EXPRESSION: Nothing()
// RESULT: 'Nothing' can't be instantiated

// EXPRESSION: "" + Nothing()
// RESULT: 'Nothing' can't be instantiated

// EXPRESSION: run { Nothing() }
// RESULT: 'Nothing' can't be instantiated

// EXPRESSION: { Nothing() }
// RESULT: 'Nothing' can't be instantiated

// EXPRESSION: block { Nothing() }
// RESULT: 'Nothing' can't be instantiated

// EXPRESSION: fun foo() { Nothing() }
// RESULT: 'Nothing' can't be instantiated