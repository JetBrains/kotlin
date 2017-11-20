// !WITH_NEW_INFERENCE
fun main(args : Array<String>) {
    val a : Int? = null;
    var v = 1
    val <!UNUSED_VARIABLE!>b<!> : String = <!TYPE_MISMATCH!>v<!>;
    val <!UNUSED_VARIABLE!>f<!> : String = <!TYPE_MISMATCH!>a<!>!!;
    val <!UNUSED_VARIABLE!>g<!> : String = <!TYPE_MISMATCH!>v++<!>;
    val <!UNUSED_VARIABLE!>g1<!> : String = <!TYPE_MISMATCH!>++v<!>;
    val <!UNUSED_VARIABLE!>h<!> : String = <!TYPE_MISMATCH!>v--<!>;
    val <!UNUSED_VARIABLE!>h1<!> : String = <!TYPE_MISMATCH!>--v<!>;
    val <!UNUSED_VARIABLE!>i<!> : String = <!TYPE_MISMATCH!>!true<!>;
    val <!UNUSED_VARIABLE!>j<!> : String = foo@ <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>;
    val <!UNUSED_VARIABLE!>k<!> : String = foo@ bar@ <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>;
    val <!UNUSED_VARIABLE!>l<!> : String = <!TYPE_MISMATCH!>-1<!>;
    val <!UNUSED_VARIABLE!>m<!> : String = <!TYPE_MISMATCH!>+1<!>;
}