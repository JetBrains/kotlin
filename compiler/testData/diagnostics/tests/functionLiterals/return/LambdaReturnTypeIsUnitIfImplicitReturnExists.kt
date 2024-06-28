// FIR_IDENTICAL
// ISSUE: KT-63563

fun foo(x: () -> Any) = x()
fun foo2(x: () -> Unit) = x()
fun <T> foo3(x: () -> T): T = x()

fun main2() {
    foo {
        if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@foo<!>
        ""
    }
    foo2 {
        if ("1".hashCode() == 42) return@foo2
        ""
    }
    foo3 { // Infer T to Unit -> no error
        if ("2".hashCode() == 42) return@foo3
        ""
    }
}

