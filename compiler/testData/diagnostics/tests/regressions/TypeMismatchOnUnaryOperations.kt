fun main(args : Array<String>) {
    val a : Int? = null;
    var v = 1
    val <!UNUSED_VARIABLE!>b<!> : String = <!TYPE_MISMATCH!>v<!>;
    val <!UNUSED_VARIABLE!>f<!> : String = <!TYPE_MISMATCH!>a!!<!>;
    val <!UNUSED_VARIABLE!>g<!> : String = <!TYPE_MISMATCH!>v++<!>;
    val <!UNUSED_VARIABLE!>g1<!> : String = <!TYPE_MISMATCH!>++v<!>;
    val <!UNUSED_VARIABLE!>h<!> : String = <!TYPE_MISMATCH!>v--<!>;
    val <!UNUSED_VARIABLE!>h1<!> : String = <!TYPE_MISMATCH!>--v<!>;
    val <!UNUSED_VARIABLE!>i<!> : String = <!TYPE_MISMATCH!>!true<!>;
    val <!UNUSED_VARIABLE!>j<!> : String = <!TYPE_MISMATCH!>@foo <!ERROR_COMPILE_TIME_VALUE!>true<!><!>;
    val <!UNUSED_VARIABLE!>j1<!> : String = <!TYPE_MISMATCH!>@ <!ERROR_COMPILE_TIME_VALUE!>true<!><!>;
    val <!UNUSED_VARIABLE!>j2<!> : String = <!TYPE_MISMATCH!>@@ <!ERROR_COMPILE_TIME_VALUE!>true<!><!>;
    val <!UNUSED_VARIABLE!>k<!> : String = <!TYPE_MISMATCH!>-1<!>;
    val <!UNUSED_VARIABLE!>l<!> : String = <!TYPE_MISMATCH!>+1<!>;
}