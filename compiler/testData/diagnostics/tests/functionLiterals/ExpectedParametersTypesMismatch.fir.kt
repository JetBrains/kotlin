// !WITH_NEW_INFERENCE
package a

fun foo0(f: () -> String) = f
fun foo1(f: (Int) -> String) = f
fun foo2(f: (Int, String) -> String) = f

fun test1() {
    foo0 {
        ""
    }
    <!INAPPLICABLE_CANDIDATE!>foo0<!> {
        s: String-> ""
    }
    <!INAPPLICABLE_CANDIDATE!>foo0<!> {
        x, y -> ""
    }

    foo1 {
        ""
    }
    <!INAPPLICABLE_CANDIDATE!>foo1<!> {
        s: String -> ""
    }
    <!INAPPLICABLE_CANDIDATE!>foo1<!> {
        x, y -> ""
    }
    foo1 {
        -> 42
    }


    foo2 {
        ""
    }
    <!INAPPLICABLE_CANDIDATE!>foo2<!> {
        s: String -> ""
    }
    <!INAPPLICABLE_CANDIDATE!>foo2<!> {
        x -> ""
    }
    foo2 {
         -> 42
    }
}
