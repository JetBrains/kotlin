interface A
interface B
interface C : A, B
interface D : A, B

fun <T> select(vararg x: T): T = x[0]

fun test(c: C, d: D) {
    val it = select(c, d)
    acceptString(<!TYPE_MISMATCH("String; {A & B}")!>it<!>)
}

fun acceptString(s: String) {}