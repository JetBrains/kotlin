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

    A()
    B()
    C(1)
    D()
    E(1)
    F("foo")
}

// EXPRESSION: 1 + 1
// RESULT: 2: I
//Breakpoint!
class A

// EXPRESSION: 1 + 2
// RESULT: 3: I
//Breakpoint!
class B()

// EXPRESSION: a
// RESULT: 1: I
//Breakpoint!
class C(val a: Int)

class D {
    // EXPRESSION: 1 + 3
    // RESULT: 4: I
    //Breakpoint!
    constructor()
}
class E {
    // EXPRESSION: i
    // RESULT: 1: I
    //Breakpoint!
    constructor(i: Int)
}

// EXPRESSION: a
// RESULT: "foo": Ljava/lang/String;
//Breakpoint!
class F(val a: String)