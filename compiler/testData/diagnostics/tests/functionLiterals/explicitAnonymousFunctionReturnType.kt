// FIR_IDENTICAL
// KT-63563 If the return type of an anonymous function is explicitly specified and the return type is not a ConeTypeParameter, then RETURN_TYPE_MISMATCH should be reported
fun foo(x: () -> Any?) {}
fun <T> fooWithTypeParameter(x: () -> T): T = x()
fun foo2(x2: () -> Any?, y2: () -> Any?) {}
fun <T> foo2WithTypeParameter(x2: () -> T, y2: () -> Any?) {}

fun test() {
    run {
        if ("".hashCode() == 1) return@run

        ""
    }
}

fun test1() {
    foo {
        if ("".hashCode() == 1) ""

        <!RETURN_TYPE_MISMATCH!>return@foo<!>
    }
}

fun test2() {
    foo {
        if ("".hashCode() == 1) <!RETURN_TYPE_MISMATCH!>return@foo<!>

        ""
    }
}

fun test3() {
    foo2WithTypeParameter(
        {
            if ("".hashCode() == 1) return@foo2WithTypeParameter

            ""
        }) {
        if ("".hashCode() == 2) <!RETURN_TYPE_MISMATCH!>return@foo2WithTypeParameter<!>

        ""
    }
}

fun test4() {
    foo2({
             if ("".hashCode() == 1) <!RETURN_TYPE_MISMATCH!>return@foo2<!>

             ""
         }) {
        if ("".hashCode() == 2) <!RETURN_TYPE_MISMATCH!>return@foo2<!>

        ""
    }
}

fun test5() {
    fooWithTypeParameter {
        if (true) return@fooWithTypeParameter
        if (true) return@fooWithTypeParameter Unit
        fooWithTypeParameter { Any() }
    }
}