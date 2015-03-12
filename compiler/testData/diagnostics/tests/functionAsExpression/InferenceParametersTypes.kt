// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
fun listOf<T>(): List<T> = null!!

fun test(a: (Int) -> Int) {
    test(fun (x) = 4)

    test(fun (x) = x)

    test(fun (x): Int { x: Int; return 4 })
}

fun test2(a: () -> List<Int>) {
    test2(fun () = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>listOf<!>())
}

val a: (Int) -> Unit = fun(x) { x: Int }

val b: (Int) -> Unit = <!TYPE_MISMATCH!>fun(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: String<!>) {}<!>