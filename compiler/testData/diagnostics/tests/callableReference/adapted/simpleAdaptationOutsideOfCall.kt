// SKIP_TXT
fun baz(options: String = ""): String = ""

fun runForString(x: () -> String) {}

fun foo(dumpStrategy: String) {
    val dump0: () -> String = <!TYPE_MISMATCH!>::<!TYPE_MISMATCH!>baz<!><!>

    runForString(::baz)
}
