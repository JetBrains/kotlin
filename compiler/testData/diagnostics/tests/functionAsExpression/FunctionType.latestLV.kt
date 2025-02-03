// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

fun testReturnType(foo: String) {
    val bar = fun () = foo

    bar.checkType { _<() -> String>() }

    val bas: () -> String = fun () = foo

    val bag: () -> Int = fun () = <!RETURN_TYPE_MISMATCH!>foo<!>
}

fun testParamType() {
    val bar = fun (bal: String){}

    bar.checkType { _<(String) -> Unit>() }

    val bas: (String) -> Unit = fun (param: String) {}
    val bag: (Int) -> Unit = <!ARGUMENT_TYPE_MISMATCH!>fun (param: String) {}<!>
}

fun testReceiverType() {
    val bar = fun String.() {}

    bar.checkType { _<String.() -> Unit>() }

    val bas: String.() -> Unit = fun String.() {}

    val bag: Int.() -> Unit = <!ARGUMENT_TYPE_MISMATCH!>fun String.() {}<!>
}
