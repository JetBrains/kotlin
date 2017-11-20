// !WITH_NEW_INFERENCE
package a

fun foo0(f: () -> String) = f
fun foo1(f: (Int) -> String) = f
fun foo2(f: (Int, String) -> String) = f

fun test1() {
    foo0 {
        ""
    }
    foo0 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!UNUSED_ANONYMOUS_PARAMETER!>s<!>: String<!>-> ""
    }
    foo0 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>y<!><!> -> ""
    }

    foo1 {
        ""
    }
    foo1 {
        <!EXPECTED_PARAMETER_TYPE_MISMATCH!><!UNUSED_ANONYMOUS_PARAMETER!>s<!>: String<!> -> ""
    }
    foo1 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!UNUSED_ANONYMOUS_PARAMETER!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>y<!><!> -> ""
    }
    foo1 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!>-> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    }


    foo2 <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!>
        ""
    }
    foo2 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH, EXPECTED_PARAMETER_TYPE_MISMATCH!><!UNUSED_ANONYMOUS_PARAMETER!>s<!>: String<!> -> ""
    }
    foo2 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH, UNUSED_ANONYMOUS_PARAMETER!>x<!> -> ""
    }
    foo2 {
         <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!>-> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>
    }
}
