val a = fun() {}
val b = fun test() {}

val c = fun() {}
val c = fun() = 4

val c = fun() =
        4

val c = fun() {}
val c = fun() = 4

fun test() = fun test() = 4

fun test() {
    test(
            fun() {
            },
    )
    test(fun test() {})
    test(fun test() = 5)

    test(fun test() = fun test(a: Int) = 2)

    2.(fun test() {})()

    (fun() = 4)
    (fun() {})

    test(fun test() = 4)
}

fun d = fun(
        a: Int,
        b: String,
) {
}

fun e = fun() {
    val a = 1
}

fun f = fun() { foo() }

val g: (String) -> Int = fun(p: String): Unit { /* comment */ /* other comment */ }

fun foo() {}