// K2: See KT-65342

fun main() {
    val a : Int? = null;
    var v = 1
    val b : String = <!TYPE_MISMATCH!>v<!>;
    val f : String = <!TYPE_MISMATCH!>a<!>!!;
    val g : String = <!TYPE_MISMATCH!>v++<!>;
    val g1 : String = <!TYPE_MISMATCH!>++v<!>;
    val h : String = <!TYPE_MISMATCH!>v--<!>;
    val h1 : String = <!TYPE_MISMATCH!>--v<!>;
    val i : String = <!TYPE_MISMATCH!>!true<!>;
    val j : String = <!REDUNDANT_LABEL_WARNING!>foo@<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>;
    val k : String = <!REDUNDANT_LABEL_WARNING!>foo@<!> <!REDUNDANT_LABEL_WARNING!>bar@<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>;
    val l : String = <!TYPE_MISMATCH!>-1<!>;
    val m : String = <!TYPE_MISMATCH!>+1<!>;
}
