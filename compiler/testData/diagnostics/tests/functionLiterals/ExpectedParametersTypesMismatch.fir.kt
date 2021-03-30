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
        s: String-> <!ARGUMENT_TYPE_MISMATCH!>""<!>
    }
    foo0 {
        x, y -> <!ARGUMENT_TYPE_MISMATCH!>""<!>
    }

    foo1 {
        ""
    }
    foo1 {
        s: String -> <!ARGUMENT_TYPE_MISMATCH!>""<!>
    }
    foo1 {
        x, y -> <!ARGUMENT_TYPE_MISMATCH!>""<!>
    }
    foo1 {
        -> 42
    }


    foo2 {
        ""
    }
    foo2 {
        s: String -> <!ARGUMENT_TYPE_MISMATCH!>""<!>
    }
    foo2 {
        x -> <!ARGUMENT_TYPE_MISMATCH!>""<!>
    }
    foo2 {
         -> 42
    }
}
