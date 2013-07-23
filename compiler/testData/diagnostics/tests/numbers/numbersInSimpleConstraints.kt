package a

fun <T> id(t: T): T = t

fun <T> either(t1: T, <!UNUSED_PARAMETER!>t2<!>: T): T = t1

fun other(<!UNUSED_PARAMETER!>s<!>: String) {}

fun <T> otherGeneric(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

fun test() {
    val <!UNUSED_VARIABLE!>a<!>: Byte = id(1)

    val <!UNUSED_VARIABLE!>b<!>: Byte = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>id<!>(300)

    val <!UNUSED_VARIABLE!>c<!>: Int = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>id<!>(9223372036854775807)

    val d = id(22)
    d: Int

    val e = id(9223372036854775807)
    <!TYPE_MISMATCH!>e<!>: Int
    e: Long

    val <!UNUSED_VARIABLE!>f<!>: Byte = either(1, 2)

    val <!UNUSED_VARIABLE!>g<!>: Byte = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>either<!>(1, 300)

    other(<!ERROR_COMPILE_TIME_VALUE!>11<!>)

    <!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>otherGeneric<!>(<!ERROR_COMPILE_TIME_VALUE!>1<!>)

    val r = either(1, "")
    <!TYPE_MISMATCH!>r<!>: Int
    <!TYPE_MISMATCH!>r<!>: String
    r: Any
}

trait Inv<T>

fun <T> exactBound(<!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>l<!>: Inv<T>) {}

fun testExactBound(invS: Inv<String>, invI: Inv<Int>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>exactBound<!>(1, invS)
    exactBound(1, invI)
}

trait Cov<out T>

fun <T> lowerBound(t: T, <!UNUSED_PARAMETER!>l<!>: Cov<T>) = t

fun testLowerBound(cov: Cov<String>) {
    val r = lowerBound(1, cov)
    r: Any
}

trait Contr<in T>

fun <T> upperBound(t: T, <!UNUSED_PARAMETER!>l<!>: Contr<T>) = t

fun testUpperBound(contrS: Contr<String>, contrB: Contr<Byte>, contrN: Contr<Number>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>upperBound<!>(1, contrS)

    upperBound(1, contrN)
    upperBound(1, contrB)
}
