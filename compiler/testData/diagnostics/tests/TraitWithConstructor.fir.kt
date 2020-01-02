// !DIAGNOSTICS: -MISSING_CONSTRUCTOR_KEYWORD

class C(val a: String) {}

interface T1(val x: String) {}

interface T2 constructor() {}

interface T3 private constructor(a: Int) {}

interface T4 {
    constructor(a: Int) {
        val b: Int = 1
    }
}

interface T5 private () : T4 {}
interface T6 private<!SYNTAX!><!> : T5 {}