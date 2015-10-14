// !DIAGNOSTICS: -UNUSED_PARAMETER

class Pair<A, B>(val a: A, val b: B)
infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

infix fun String.o1(o: String) = o
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.o2(o: String, o2: String? = null) = o
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.o3(o: String = "", o2: String? = null) = o

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun w1() {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun w2(s: String) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.w3() {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun String.w4(a: Int, b: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun w5(a: Int, b: Int) {}

class Example {
    infix fun c1(s: String) {}
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun c2(s: String, a: Int = 0) {}

    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun cw1(s: String, a: Int) {}
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun sw2() {}
}