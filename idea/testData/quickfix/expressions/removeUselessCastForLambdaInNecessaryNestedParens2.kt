// "Remove useless cast" "true"
fun foo() {}

fun test() {
    foo()
    // comment
    ((({ "" } as<caret> () -> String)))
}
/* IGNORE_FIR */