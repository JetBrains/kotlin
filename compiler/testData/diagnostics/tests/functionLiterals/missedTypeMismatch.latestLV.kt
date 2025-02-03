// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNCHECKED_CAST
// Issues: KT-38890, KT-38439

fun foo(x: () -> Int) {}

fun <T>id(x: T) = x

// Before the fix, there wasn't any type mismatch error in NI due to result type not being a subtype of expected type
fun main() {
    val x: () -> Int = { <!RETURN_TYPE_MISMATCH!>""<!> }

    val a0: () -> Int = <!ARGUMENT_TYPE_MISMATCH!>fun(): String = "1"<!>
    val a1: () -> Int = (fun() = <!RETURN_TYPE_MISMATCH!>"1"<!>)
    val a2: () -> Unit = (fun() = <!RETURN_TYPE_MISMATCH!>"1"<!>)
    val a3: Unit = <!INITIALIZER_TYPE_MISMATCH!>(fun() = "1")<!>
    val a4 = (fun() = "1")
    val a5 = (fun(): String = "1")
    val a6: () -> Int = (fun() = 1)
    val a7: () -> Int = (fun(): String = "1") as () -> Int
    val a8: () -> Int = <!ARGUMENT_TYPE_MISMATCH!>fun(): String = "1"<!>
    val a9: () -> () -> () -> Int = <!ARGUMENT_TYPE_MISMATCH!>fun(): () -> () -> String = fun(): () -> String = fun(): String = "1"<!>

    foo(<!ARGUMENT_TYPE_MISMATCH!>fun(): String = "1"<!>)
    foo(((<!ARGUMENT_TYPE_MISMATCH!>fun(): String = "1"<!>)))

    val a10: Int.(String) -> Int = <!ARGUMENT_TYPE_MISMATCH!>fun (x: String) = 10<!>
    val a11: () -> () -> () -> Int = fun() = fun() = <!RETURN_TYPE_MISMATCH!>fun(): String = "1"<!>

    val a12: Int = <!ARGUMENT_TYPE_MISMATCH!>fun(): String = "1"<!>
    val a13: Int = <!ARGUMENT_TYPE_MISMATCH!>fun() = fun(): String = "1"<!>
    val a14: Int = <!ARGUMENT_TYPE_MISMATCH!>fun() = fun() = "1"<!>
    val a15: Int = <!ARGUMENT_TYPE_MISMATCH!>fun() = fun() = {}<!>
    val a16: Int = <!ARGUMENT_TYPE_MISMATCH!>fun() = fun() {}<!>

    val a17: () -> Unit = fun() {}
    val a18: () -> Int = <!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>
    val a19: () -> () -> Int = fun() = <!RETURN_TYPE_MISMATCH!>fun() {}<!>
    val a20: () -> () -> () -> Unit = fun() = fun() = {}
    val a21: () -> () -> () -> Int = fun() = fun() = <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>{}<!>
}
