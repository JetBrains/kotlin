package a

fun foo0(f: () -> String) = f
fun foo1(f: (Int) -> String) = f
fun foo2(f: (Int, String) -> String) = f

fun test1() {
    foo0 {
        ""
    }
    foo0 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>s: String<!>-> ""
    }<!>
    foo0 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!><!> -> ""
    }<!>

    foo1 {
        ""
    }
    foo1 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETER_TYPE_MISMATCH!>s: String<!> -> ""
    }<!>
    foo1 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!><!> -> ""
    }<!>
    foo1 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!>-> <!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    }<!>


    foo2 <!TYPE_MISMATCH!><!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!>
        ""
    }<!>
    foo2 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH, EXPECTED_PARAMETER_TYPE_MISMATCH!>s: String<!> -> ""
    }<!>
    foo2 <!TYPE_MISMATCH!>{
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> ""
    }<!>
    foo2 <!TYPE_MISMATCH!>{
         <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!>-> <!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    }<!>
}
