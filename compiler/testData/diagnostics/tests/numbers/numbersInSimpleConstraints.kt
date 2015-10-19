// !CHECK_TYPE
package a

fun <T> id(t: T): T = t

fun <T> either(t1: T, <!UNUSED_PARAMETER!>t2<!>: T): T = t1

fun other(<!UNUSED_PARAMETER!>s<!>: String) {}

fun <T> otherGeneric(<!UNUSED_PARAMETER!>l<!>: List<T>) {}

fun test() {
    val a: Byte = id(1)

    val b: Byte = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>id(300)<!>

    val c: Int = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>id(9223372036854775807)<!>

    val d = id(22)
    checkSubtype<Int>(d)

    val e = id(9223372036854775807)
    e checkType { _<Long>() }

    val f: Byte = either(1, 2)

    val g: Byte = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>either(1, 300)<!>

    other(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>11<!>)

    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>otherGeneric<!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)

    val r = either(1, "")
    r checkType { _<Any>() }

    use(a, b, c, d, e, f, g, r)
}

fun use(vararg a: Any?) = a

interface Inv<T>

fun <T> exactBound(t: T, l: Inv<T>): T = throw Exception("$t $l")

fun testExactBound(invS: Inv<String>, invI: Inv<Int>, invB: Inv<Byte>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>exactBound<!>(1, invS)
    exactBound(1, invI)

    val b = exactBound(1, invB)
    b checkType { _<Byte>() }
}

interface Cov<out T>

fun <T> lowerBound(t: T, l : Cov<T>): T = throw Exception("$t $l")

fun testLowerBound(cov: Cov<String>, covN: Cov<Number>) {
    val r = lowerBound(1, cov)
    r checkType { _<Any>() }

    val n = lowerBound(1, covN)
    n checkType { _<Number>() }
}

interface Contr<in T>

fun <T> upperBound(t: T, l: Contr<T>): T = throw Exception("$t $l")

fun testUpperBound(contrS: Contr<String>, contrB: Contr<Byte>, contrN: Contr<Number>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>upperBound<!>(1, contrS)

    val n = upperBound(1, contrN)
    n checkType { _<Int>() }

    val b = upperBound(1, contrB)
    b checkType { _<Byte>() }
}
