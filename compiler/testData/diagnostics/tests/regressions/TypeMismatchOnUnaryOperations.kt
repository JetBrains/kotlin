// !WITH_NEW_INFERENCE
fun main() {
    val a : Int? = null;
    var v = 1
    val <!UNUSED_VARIABLE!>b<!> : String = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>v<!>;
    val <!UNUSED_VARIABLE!>f<!> : String = <!NI;TYPE_MISMATCH!><!OI;TYPE_MISMATCH!>a<!>!!<!>;
    val <!UNUSED_VARIABLE!>g<!> : String = <!TYPE_MISMATCH!>v++<!>;
    val <!UNUSED_VARIABLE!>g1<!> : String = <!TYPE_MISMATCH!>++v<!>;
    val <!UNUSED_VARIABLE!>h<!> : String = <!TYPE_MISMATCH!>v--<!>;
    val <!UNUSED_VARIABLE!>h1<!> : String = <!TYPE_MISMATCH!>--v<!>;
    val <!UNUSED_VARIABLE!>i<!> : String = <!TYPE_MISMATCH!>!true<!>;
    val <!UNUSED_VARIABLE!>j<!> : String = <!REDUNDANT_LABEL_WARNING!>foo@<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>;
    val <!UNUSED_VARIABLE!>k<!> : String = <!REDUNDANT_LABEL_WARNING!>foo@<!> <!REDUNDANT_LABEL_WARNING!>bar@<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>;
    val <!UNUSED_VARIABLE!>l<!> : String = <!TYPE_MISMATCH!>-1<!>;
    val <!UNUSED_VARIABLE!>m<!> : String = <!TYPE_MISMATCH!>+1<!>;
}