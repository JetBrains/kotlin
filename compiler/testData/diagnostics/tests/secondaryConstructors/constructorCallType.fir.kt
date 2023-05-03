// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE
class A(x: Int) {
    constructor(x: Double): this(1)
    constructor(x: String): this(1)
}
val x1: A = A(1)
val x2: A = A(1.0)
val x3: A = A("abc")

class B<R> {
    constructor(x: String)
    constructor(x: R)
}

val y1: B<Int> = B(1)
val y2: B<Int> = B("")
val y3: B<Int> = B<Int>(1)
val y4: B<Int> = B<Int>("")

val y5: B<String> = B<String>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
val y6: B<String> = B<String>("")
val y7: B<String> = <!TYPE_MISMATCH, TYPE_MISMATCH!>B(1)<!>
val y8: B<String> = B("")

val y9 = B(1)
val y10 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>B<!>("")
