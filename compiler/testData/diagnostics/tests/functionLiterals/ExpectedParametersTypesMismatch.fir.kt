// !WITH_NEW_INFERENCE
package a

fun foo0(f: () -> String) = f
fun foo1(f: (Int) -> String) = f
fun foo2(f: (Int, String) -> String) = f

fun test1() {
    foo0 {
        ""
    }
    foo0 <!ARGUMENT_TYPE_MISMATCH!>{
        s: String-> ""
    }<!>
    foo0 <!ARGUMENT_TYPE_MISMATCH!>{
        <!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> ""
    }<!>

    foo1 {
        ""
    }
    foo1 <!ARGUMENT_TYPE_MISMATCH!>{
        s: String -> ""
    }<!>
    foo1 <!ARGUMENT_TYPE_MISMATCH!>{
        x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> ""
    }<!>
    foo1 {
        -> <!ARGUMENT_TYPE_MISMATCH!>42<!>
    }


    foo2 <!ARGUMENT_TYPE_MISMATCH!>{
        ""
    }<!>
    foo2 <!ARGUMENT_TYPE_MISMATCH!>{
        s: String -> ""
    }<!>
    foo2 <!ARGUMENT_TYPE_MISMATCH!>{
        x -> ""
    }<!>
    foo2 <!ARGUMENT_TYPE_MISMATCH!>{
         -> <!ARGUMENT_TYPE_MISMATCH!>42<!>
    }<!>
}
