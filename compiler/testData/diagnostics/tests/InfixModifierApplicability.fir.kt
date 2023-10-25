// !DIAGNOSTICS: -UNUSED_PARAMETER

class Pair<A, B>(val a: A, val b: B)
infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

// OK
infix fun String.ok1(o: String) {}
class OkTest {
    infix fun ok2(o: String) {}
    infix fun String.ok3(o: String) {}
}

// Errors
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.e1(o: String, o2: String? = null) = o
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.e2(o: String = "", o2: String? = null) = o

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun e3() {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun e4(s: String) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.e5() {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.e6(a: Int, b: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun e7(a: Int, b: Int) {}

class Example {
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun e8(s: String, a: Int = 0) {}
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun e9(s: String, a: Int) {}
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun e10() {}
}
