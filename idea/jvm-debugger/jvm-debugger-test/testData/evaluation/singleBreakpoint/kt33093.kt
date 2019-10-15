package kt33093

typealias foo = Nothing
typealias bar = foo

fun main() {
    //Breakpoint!
    Unit
}

// EXPRESSION: Nothing()
// RESULT: Type 'Nothing' can't be instantiated

// EXPRESSION: foo()
// RESULT: Type 'Nothing' can't be instantiated

// EXPRESSION: bar()
// RESULT: Type 'Nothing' can't be instantiated

// EXPRESSION: { Nothing() }
// RESULT: Type 'Nothing' can't be instantiated

// EXPRESSION: { bar() }
// RESULT: Type 'Nothing' can't be instantiated