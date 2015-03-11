// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(x: Int) {
    constructor(x: Double): this(1) {}
    constructor(x: String): this(1) {}
}
val x1: A = A(1)
val x2: A = A(1.0)
val x3: A = A("abc")

class B<R> {
    constructor(x: String) {}
    constructor(x: R) {}
}

val y1: B<Int> = B(1)
val y2: B<Int> = B("")
val y3: B<Int> = B<Int>(1)
val y4: B<Int> = B<Int>("")

val y5: B<String> = <!NONE_APPLICABLE!>B<!><String>(1)
val y6: B<String> = <!OVERLOAD_RESOLUTION_AMBIGUITY!>B<!><String>("") // TODO: doesn't work here but ok on y8
val y7: B<String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>B<!>(1)
val y8: B<String> = B("")

val y9 = B(1)
val y10 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>B<!>("")
