// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER -UNUSED_PARAMETER -UNUSED_EXPRESSION

class Sample

fun <K> id(x: K): K = x

fun test() {
    val f00: Sample.() -> Unit = id { val a = 1 }
    val f01: Sample.() -> Unit = id { s: Sample -> }
    val f02: Sample.() -> Unit = id<Sample.() -> Unit> { s: Sample -> }
}

enum class E { VALUE }

typealias E0 = Int.() -> Int
class W1(val f: E0) {
    // overload ambiguity is not supported yet - see commented examples with "overload" keyword below
//    constructor(f: () -> Int) : this(fun Int.(): Int = f() )
}

typealias E1 = Int.(String) -> Int
class W2(val f: E1) {
    // overload ambiguity is not supported yet - see commented examples with "overload" keyword below
//    constructor(f: Int.() -> Int) : this(fun Int.(String): Int = f())
}

typealias L1 = (Int) -> Int
class W3(val f: L1) {
    // overload ambiguity is not supported yet - see commented examples with "overload" keyword below
//    constructor(f: () -> Int) : this( { i: Int -> f() } )
}

typealias L2 = (Int, String) -> Int
class W4(val f: L2) {
    // overload ambiguity is not supported yet - see commented examples with "overload" keyword below
//    constructor(f: L1) : this( { i: Int, s: String -> f(i) } )
}

fun test1() { // to extension lambda 0
    val w10 = W1 { this } // oi+ ni+
    val i10: E0 = id { this } // o1- ni+
    val j10 = id<E0> { this } // oi+ ni+
    val f10 = W1(fun Int.(): Int = this) // oi+ ni+
    val g10: E0 = id(fun Int.(): Int = this) // oi+ ni+

    val w11 = W1 <!ARGUMENT_TYPE_MISMATCH!>{ i: Int -> i }<!> // oi- ni-
    val i11: E0 = id { i: Int -> i } // o1+ ni+
    val w12 = W1 <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>i<!> -> i }<!> // oi- ni-
    val i12: E0 = id <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>i<!> -> i }<!> // oi- ni-
    val j12 = id<E0> <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>i<!> -> i }<!> // oi- ni-

    // yet unsupported cases - considering lambdas as extension ones unconditionally
//    val w13 = W1 { it } // this or it: oi- ni-
//    val i13: E0 = id { it } // this or it: oi- ni-
//    val j13 = id<E0> { it } // this or it: oi- ni-

    val o14 = W1 { -> 0 } // oi+ ni+
}

fun test2() { // to extension lambda 1
    val w20 = W2 { this + it.length } // oi+ ni+
    val i20: E1 = id { this + it.length } // oi- ni+
    val w21 = W2 { this } // oi+ ni+
    val i21: E1 = id { this } // oi- ni+
    val f21 = W2(fun Int.(String): Int = this) // oi+ ni+
    val g21: E1 = id(fun Int.(String): Int = this) // oi+ ni+
    val w22 = W2 { s -> this + s.length } // oi+ ni+
    val i22: E1 = id { s -> this + s.length } // oi+ ni+
    val w23 = W2 { s -> s.length } // oi+ ni+
    val i23: E1 = id { s -> s.length } // oi+ ni+
    val w24 = W2 { s: String -> this + s.length } // oi+ ni+
    val i24: E1 = id { s: String -> this + s.length } //oi- ni+
    val w25 = W2 { s: String -> s.length } // oi+ ni+
    val i25: E1 = id { s: String -> s.length } // oi- ni+
    val w26 = W2(id { s: String -> this + s.length }) // oi- ni+
    val w26a = W2(id { s -> this + s.length }) // oi+ ni+
    val i26: E1 = id { s: String -> this + s.length } // oi- ni+
    val i26a: E1 = id { s -> this + s.length } // oi+ ni+
    val e = E.VALUE
    val w27 = W2(<!ARGUMENT_TYPE_MISMATCH!>when (e) { E.VALUE ->  { s: String -> <!NO_THIS!>this<!> + s.length } }<!>) // oi- ni+
    val w27a = W2(<!ARGUMENT_TYPE_MISMATCH!>when (e) { E.VALUE ->  { s -> <!NO_THIS!>this<!> + s.<!UNRESOLVED_REFERENCE!>length<!> } }<!>) // oi+ ni+
    val i27: E1 = <!INITIALIZER_TYPE_MISMATCH!>when (e) { E.VALUE ->  { s: String -> <!NO_THIS!>this<!> + s.length } }<!> // oi+ ni+
    val i27a: E1 = <!INITIALIZER_TYPE_MISMATCH!>when (e) { E.VALUE ->  { s -> <!NO_THIS!>this<!> + s.<!UNRESOLVED_REFERENCE!>length<!> } }<!> // oi+ ni+

    val w28 = W2 <!ARGUMENT_TYPE_MISMATCH!>{ i: Int, <!CANNOT_INFER_PARAMETER_TYPE!>s<!> -> i <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> s.<!UNRESOLVED_REFERENCE!>length<!> }<!> // oi- ni-
    val i28: E1 = id { i: Int, <!CANNOT_INFER_PARAMETER_TYPE!>s<!> -> <!ARGUMENT_TYPE_MISMATCH!>i <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> s.<!UNRESOLVED_REFERENCE!>length<!><!> } // oi- ni-
    val w29 = W2 <!ARGUMENT_TYPE_MISMATCH!>{ i: Int, s: String -> i + s.length }<!> // oi- ni-
    val i29: E1 = id { i: Int, s: String -> i + s.length } // oi+ ni+

    // yet unsupported cases with ambiguity for the lambda conversion (commented constructors in wrappers above)
//    val w2a = W2 { i, s -> i + s.length } // overload oi- ni-
//    val i2a: E1 = id { i, s -> i + s.length } // overload oi- ni-
//    val w2b = W2 { i, s: String -> i + s.length } // overload oi- ni-
//    val i2b: E1 = id { i, s: String -> i + s.length } // overload oi- ni-

    // yet unsupported cases with ambiguity for the lambda conversion (commented constructors in wrappers above)
//    val o2c = W2 { i: Int -> i } // overload oi- ni+
//    val o2d = W2 { i -> i } // overload oi- ni-
}

fun test3() { // to non-extension lambda 1
    val w30 = W3 { i -> i } // oi+ ni+
    val i30: L1 = id { i -> i } // oi+ ni+
    val w31 = W3 { it } // oi+ ni+
    val i31: L1 = id { it } // oi- ni+
    val j31 = id<L1> { it } // oi+ ni+

    // yet unsupported cases - considering lambdas as extension ones unconditionally
//    val w32 = W3 { this } // this or it: oi- ni-
//    val i32: L1 = id { this } // this or it: oi- ni-
//    val j32 = id<L1> { this } // this or it: oi- ni-

    val w33 = W3(fun Int.(): Int = this) // oi- ni+
    val i33: L1 = id(fun Int.(): Int = this) // oi+ ni+

    // yet unsupported cases with ambiguity for the lambda conversion (commented constructors in wrappers above)
//    val o34 = W3 { -> 1 } // overload oi- ni-
}

fun test4() { // to non-extension lambda 2
    val w30 = W4 { i, s -> i } // oi+ ni+
    val i30: L2 = id { i, s -> i } // oi+ ni+

    // yet unsupported cases with ambiguity for the lambda conversion (commented constructors in wrappers above)
//    val w31 = W4 { this } // overload oi- ni-
//    val i31: L2 = id { this } // overload oi- ni-
//    val w32 = W4 { this + it.length } // overload oi- ni-
//    val i32: L2 = id { this + it.length } // overload oi- ni-
}

open class A(a: () -> Unit) {
    constructor(f: (String) -> Unit) : <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>({ -> f("") })
}

class B: A({ s -> "1" })
