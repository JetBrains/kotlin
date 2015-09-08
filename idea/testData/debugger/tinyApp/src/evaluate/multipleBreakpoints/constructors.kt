package constructors

class Derived2(): Base(1) {

    // constructor with body
    // EXPRESSION: p
    // RESULT: 1: I
    //Breakpoint!
    constructor(p: Int): this() {
        // EXPRESSION: p + 1
        // RESULT: 2: I
        //Breakpoint!
        val a = 1
    }

    // constructor without body
    // EXPRESSION: p1 + p2
    // RESULT: 2: I
    //Breakpoint!
    constructor(p1: Int, p2: Int): this()
}

// EXPRESSION: i1
// RESULT: 1: I
class Derived1(
        i1: Int
        //Breakpoint!
): Base(i1)

open class Base(i: Int)

fun main(args: Array<String>) {
    Derived2(1)
    Derived2(1, 1)

    Derived1(1)
}